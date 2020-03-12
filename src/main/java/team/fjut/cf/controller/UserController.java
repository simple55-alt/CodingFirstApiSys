package team.fjut.cf.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import team.fjut.cf.component.interceptor.LoginRequired;
import team.fjut.cf.component.interceptor.PrivateRequired;
import team.fjut.cf.component.jwt.JwtTokenManager;
import team.fjut.cf.component.token.TokenModel;
import team.fjut.cf.pojo.enums.ResultJsonCode;
import team.fjut.cf.pojo.po.UserAuth;
import team.fjut.cf.pojo.po.UserBaseInfo;
import team.fjut.cf.pojo.po.UserCustomInfo;
import team.fjut.cf.pojo.vo.ResultJsonVO;
import team.fjut.cf.pojo.vo.UserCustomInfoVO;
import team.fjut.cf.service.*;
import team.fjut.cf.util.JsonFileUtils;
import team.fjut.cf.util.IpUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @author axiang [2019/10/11]
 */
@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {
    @Autowired
    UserInfoService userInfoService;

    @Autowired
    UserAuthService userAuthService;

    @Autowired
    UserCheckInService userCheckInService;

    @Autowired
    JudgeStatusService judgeStatusService;

    @Autowired
    BorderHonorRankService borderHonorRankService;

    @Autowired
    ChallengeBlockService challengeBlockService;

    @Autowired
    JwtTokenManager jwtTokenManager;

    @PostMapping("/login")
    public ResultJsonVO userLogin(HttpServletRequest request,
                                  @RequestParam("username") String username,
                                  @RequestParam("password") String password) {
        ResultJsonVO resultJsonVO = new ResultJsonVO();
        Date currentDate = new Date();
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "用户名或者密码为空！");
            return resultJsonVO;
        }
        // 用户名不存在
        if (!userInfoService.isUsernameExist(username)) {
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "登录失败！用户不存在！");
            return resultJsonVO;
        }
        // 查询账号是否激活
        boolean isUserLocked = userAuthService.isUserLocked(username);
        if (isUserLocked) {
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "您的账号还未激活，请到邮箱中点击激活！");
            return resultJsonVO;
        }
        // 查询登录权限的解锁时间
        Date unlockTime = userAuthService.selectUnlockTime(username);
        // 如果当前时间小于解锁时间，则表示账号还在锁定期，无法登录
        if (0 > currentDate.compareTo(unlockTime)) {
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "您的账号已暂时被锁定，请稍后登录。如有疑问，请联系管理员");
            return resultJsonVO;
        }
        if (userInfoService.userLogin(username, password)) {
            resultJsonVO.setStatus(ResultJsonCode.REQUIRED_SUCCESS, "登录成功！");
            UserCustomInfoVO userCustomInfoVO = userInfoService.selectUserCustomInfo(username);
            TokenModel tokenModel = new TokenModel();
            tokenModel.setIp(IpUtils.getClientIpAddress(request));
            tokenModel.setUsername(username);
            tokenModel.setRole("Undefined");
            String token = jwtTokenManager.createToken(tokenModel);
            resultJsonVO.addInfo(username);
            resultJsonVO.addInfo(token);
            resultJsonVO.addInfo(userCustomInfoVO);
        } else {
            Integer attemptCount = userAuthService.selectAttemptNumber(username);
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "登录失败！账号或密码不正确！您还有 " + (Math.max(5 - attemptCount, 0)) + "次机会");
        }
        return resultJsonVO;
    }

    @PostMapping("/register")
    public ResultJsonVO userRegister(@RequestParam("username") String username,
                                     @RequestParam("password") String password,
                                     @RequestParam("nickname") String nickname,
                                     @RequestParam("gender") Integer gender,
                                     @RequestParam("email") String email,
                                     @RequestParam("phone") String phone,
                                     @RequestParam("motto") String motto,
                                     @RequestParam("avatarUrl") String avatarUrl) {
        ResultJsonVO resultJsonVO = new ResultJsonVO();
        Boolean isExist = userInfoService.isUsernameExist(username);
        if (isExist) {
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "注册的用户已存在！");
            return resultJsonVO;
        }
        boolean isRestrict = JsonFileUtils.isUsernameRestrict(username);
        if (isRestrict) {
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "用户名中含有关键字，请选择新的用户名后重试");
            return resultJsonVO;
        }
        UserBaseInfo userBaseInfo = new UserBaseInfo();
        userBaseInfo.setUsername(username);
        userBaseInfo.setGender(gender);
        userBaseInfo.setEmail(email);
        userBaseInfo.setPhone(phone);
        userBaseInfo.setMotto(motto);
        userBaseInfo.setRegisterTime(new Date());
        userBaseInfo.setRating(0);
        userBaseInfo.setRanking(0);
        userBaseInfo.setAcNum(0);
        userBaseInfo.setAcb(0);
        UserAuth userAuth = new UserAuth();
        userAuth.setPassword(password);
        UserCustomInfo userCustomInfo = new UserCustomInfo();
        userCustomInfo.setAvatarUrl(avatarUrl);
        userCustomInfo.setNickname(nickname);
        Boolean ans = userInfoService.registerUser(userBaseInfo, userAuth, userCustomInfo);

        if (ans) {
            resultJsonVO.setStatus(ResultJsonCode.REQUIRED_SUCCESS, "用户注册成功！");
            //// 插入挑战模式解锁记录
            //ChallengeUserOpenBlockPO challengeUserOpenBlockPO = new ChallengeUserOpenBlockPO();
            //challengeUserOpenBlockPO.setUsername(username);
            //challengeUserOpenBlockPO.setBlockId(1);
            //challengeUserOpenBlockPO.setUnlockTime(new Date());
            //challengeBlockService.unlockBlock(challengeUserOpenBlockPO);
        } else {
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "用户注册失败！");
        }
        return resultJsonVO;
    }

    @PrivateRequired
    @PostMapping("/logout")
    public ResultJsonVO userLogOut(String username) {
        ResultJsonVO resultJsonVO = new ResultJsonVO(ResultJsonCode.REQUIRED_SUCCESS);
        jwtTokenManager.deleteToken(username);
        return resultJsonVO;
    }

    @LoginRequired
    @PostMapping("/info")
    public ResultJsonVO getUserInfo(@RequestParam("username") String username) {
        ResultJsonVO resultJsonVO = new ResultJsonVO();
        UserBaseInfo userBaseInfoVO = userInfoService.selectByUsername(username);
        UserCustomInfoVO userCustomInfoVO = userInfoService.selectUserCustomInfo(username);
        Integer totalSubmit = judgeStatusService.selectCountByUsername(username);
        resultJsonVO.addInfo(userBaseInfoVO);
        resultJsonVO.addInfo(userCustomInfoVO);
        resultJsonVO.addInfo(totalSubmit);
        return resultJsonVO;
    }


    @PostMapping("/info/custom")
    public ResultJsonVO getUserCustomerInfo(@RequestParam("username") String username) {
        ResultJsonVO resultJsonVO = new ResultJsonVO();
        UserCustomInfoVO userCustomInfoVO = userInfoService.selectUserCustomInfo(username);
        resultJsonVO.addInfo(userCustomInfoVO);
        return resultJsonVO;
    }

    @LoginRequired
    @PostMapping("/award")
    public ResultJsonVO getUserAwardList(@RequestParam("username") String username) {
        ResultJsonVO resultJsonVO = new ResultJsonVO();
        List<String> awardStr = borderHonorRankService.selectByUsername(username);
        resultJsonVO.addInfo(awardStr);
        return resultJsonVO;
    }

}




