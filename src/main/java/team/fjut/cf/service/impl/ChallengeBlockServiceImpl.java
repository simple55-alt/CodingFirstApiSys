package team.fjut.cf.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.fjut.cf.mapper.ChallengeBlockConditionMapper;
import team.fjut.cf.mapper.ChallengeBlockMapper;
import team.fjut.cf.mapper.ChallengeBlockProblemMapper;
import team.fjut.cf.mapper.ChallengeUserOpenBlockMapper;
import team.fjut.cf.pojo.enums.ChallengeBlockType;
import team.fjut.cf.pojo.po.ChallengeBlockConditionPO;
import team.fjut.cf.pojo.po.ChallengeBlockPO;
import team.fjut.cf.pojo.po.ChallengeUserOpenBlockPO;
import team.fjut.cf.pojo.vo.ChallengeBlockConditionVO;
import team.fjut.cf.pojo.vo.ChallengeBlockVO;
import team.fjut.cf.pojo.vo.UserChallengeBlockVO;
import team.fjut.cf.service.ChallengeBlockService;
import team.fjut.cf.service.UserMessageService;

import java.util.*;

/**
 * @author axiang [2019/11/11]
 */
@Service
public class ChallengeBlockServiceImpl implements ChallengeBlockService {
    @Autowired
    ChallengeBlockMapper challengeBlockMapper;

    @Autowired
    ChallengeBlockProblemMapper challengeBlockProblemMapper;

    @Autowired
    ChallengeBlockConditionMapper challengeBlockConditionMapper;

    @Autowired
    ChallengeUserOpenBlockMapper challengeUserOpenBlockMapper;

    @Autowired
    UserMessageService userMessageService;


    @Override
    public Integer unlockBlock(ChallengeUserOpenBlockPO challengeUserOpenBlockPO) {
        Integer integer = challengeUserOpenBlockMapper.insert(challengeUserOpenBlockPO);
        // FIXME: 站内消息表更改
        //UserMessage userMessage = new UserMessage();
        //userMessage.setUsername(challengeUserOpenBlockPO.getUsername());
        //userMessage.setTime(challengeUserOpenBlockPO.getUnlockTime());
        //userMessage.setTitle("恭喜您解锁了新的挑战模式模块！");
        //userMessage.setText("恭喜您解锁了新的挑战模式模块，快进去看看吧");
        //userMessage.setStatus(0);
        //userMessageService.insert(userMessage);
        return integer;
    }

    @Override
    public List<UserChallengeBlockVO> selectByUsername(String username) {
        List<UserChallengeBlockVO> result = new ArrayList<>();
        // 取得用户的全部挑战模块
        List<UserChallengeBlockVO> allBlocks = challengeBlockMapper.allAsVO();
        // 将结果集放入map中
        Map<Integer, UserChallengeBlockVO> map = new TreeMap<>();
        // 锁定全部结果集
        for (UserChallengeBlockVO Block : allBlocks) {
            Block.setLocked(true);
            map.put(Block.getId(), Block);
        }
        // 获取用户已经解锁的模块ID
        List<Integer> openBlocks = challengeUserOpenBlockMapper.selectByUsername(username);
        for (Integer openBlock : openBlocks) {
            // 在map中取出并设置解锁
            UserChallengeBlockVO temp = map.get(openBlock);
            temp.setLocked(false);
            map.put(openBlock, temp);
        }
        // 获取模块的得分记录
        List<UserChallengeBlockVO> blockScoredS = challengeBlockProblemMapper.selectScoredByUsername(username);

        for (UserChallengeBlockVO blockScored : blockScoredS) {
            UserChallengeBlockVO temp = map.get(blockScored.getId());
            temp.setGetScore(blockScored.getGetScore());
            map.put(blockScored.getId(), temp);
        }
        // 获取用户可以展示的结果集的ID集合
        List<Integer> showedIds = challengeBlockMapper.selectCanShowedBlockIdByUsername(username);
        for (Integer key : map.keySet()) {
            if (showedIds.contains(key)) {
                result.add(map.get(key));
            }
        }

        return result;
    }

    @Override
    public ChallengeBlockVO selectByBlockIdAndUsername(Integer blockId, String username) {
        ChallengeBlockVO result = new ChallengeBlockVO();
        ChallengeBlockPO challengeBlockPO = challengeBlockMapper.selectByBlockId(blockId);
        if (null == challengeBlockPO) {
            return null;
        }
        result.setId(challengeBlockPO.getId());
        result.setName(challengeBlockPO.getName());
        result.setDescription(challengeBlockPO.getDescription());
        result.setBlockType(ChallengeBlockType.getNameByID(challengeBlockPO.getBlockType()));
        Integer totalScore = challengeBlockProblemMapper.selectTotalScoreByBlockId(blockId);
        result.setTotalScore(totalScore);
        Integer getScore = challengeBlockProblemMapper.selectScoredByBlockIdAndUsername(blockId, username);
        result.setGetScore(getScore);
        return result;
    }

    @Override
    public List<ChallengeBlockConditionPO> selectConditions() {
        return challengeBlockConditionMapper.all();
    }

    @Override
    public List<ChallengeBlockConditionVO> selectConditionByBlockId(Integer blockId) {
        return challengeBlockConditionMapper.selectConditionByBlockId(blockId);
    }

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void updateOpenBlock(String username, Integer problemId) {
        Date currentTime = new Date();
        // 获取该题隶属挑战模块，以下逻辑的前提条件是该题只属于一个模块
        Integer blockId = challengeBlockProblemMapper.selectByProblemId(problemId);
        // 如果题目不属于任何挑战模式模块，则返回
        if (null == blockId) {
            return;
        }
        // 获取题目隶属挑战模块的后置模块
        List<Integer> belongBlocks = challengeBlockConditionMapper.selectRearBlocksByBlockId(blockId);
        // 遍历这些后置模块，依次取出他们的各个条件，逐一判断是否满足
        for (Integer belongBlock : belongBlocks) {
            boolean canOpen = true;
            // 取出解锁条件
            List<ChallengeBlockConditionVO> conditionVOS = challengeBlockConditionMapper.selectConditionByBlockId(belongBlock);
            // 遍历条件看是否全部满足，如果有部分不满足则标识位 canOpen = false
            for (ChallengeBlockConditionVO conditionBlock : conditionVOS) {
                // 取得模块获得分数
                Integer score = challengeBlockProblemMapper.selectScoredByBlockIdAndUsername(conditionBlock.getBlockId(), username);
                if (null == score || score < conditionBlock.getNum()) {
                    canOpen = false;
                    break;
                }
            }
            if (canOpen) {
                ChallengeUserOpenBlockPO userOpenBlockPO = new ChallengeUserOpenBlockPO();
                userOpenBlockPO.setBlockId(belongBlock);
                userOpenBlockPO.setUsername(username);
                userOpenBlockPO.setUnlockTime(currentTime);
                challengeUserOpenBlockMapper.insert(userOpenBlockPO);
            }
        }

    }
}
