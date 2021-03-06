package team.fjut.cf.service;

import team.fjut.cf.pojo.vo.ChallengeBlockProblemVO;

import java.util.List;

/**
 * @author axiang [2019/11/11]
 */
public interface ChallengeBlockProblemService {
    /**
     * 分页查询模块内的题目
     *
     * @param username
     * @param blockId
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<ChallengeBlockProblemVO> pagesByBlockId(String username,
                                                 Integer blockId,
                                                 Integer pageNum,
                                                 Integer pageSize);

    /**
     * 查询模块内题目的总数量
     *
     * @param blockId
     * @return
     */
    Integer selectCountByBlockId(Integer blockId);
}
