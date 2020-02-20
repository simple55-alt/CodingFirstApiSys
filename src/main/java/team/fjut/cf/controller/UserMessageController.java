package team.fjut.cf.controller;

import team.fjut.cf.component.interceptor.PrivateRequired;
import team.fjut.cf.pojo.enums.ResultJsonCode;
import team.fjut.cf.pojo.po.UserMessagePO;
import team.fjut.cf.pojo.vo.ResultJsonVO;
import team.fjut.cf.service.UserMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author axiang [2019/11/11]
 */
@RestController
@CrossOrigin
@RequestMapping("/user_message")
public class UserMessageController {
    @Autowired
    UserMessageService userMessageService;

    @PrivateRequired
    @GetMapping("/all")
    public ResultJsonVO getUserMessageByUsername(@RequestParam("username") String username,
                                                 @RequestParam("pageNum") Integer pageNum,
                                                 @RequestParam("pageSize") Integer pageSize) {
        ResultJsonVO resultJsonVO = new ResultJsonVO(ResultJsonCode.REQUIRED_SUCCESS);
        List<UserMessagePO> userMessagePOS = userMessageService.pagesByUsername(username, pageNum, pageSize);
        Integer count = userMessageService.selectCountUserMessageByUsername(username);
        resultJsonVO.addInfo(userMessagePOS);
        resultJsonVO.addInfo(count);
        return resultJsonVO;
    }

    @PrivateRequired
    @GetMapping("/unread")
    public ResultJsonVO getUserUnreadMessageByUsername(@RequestParam("username") String username,
                                                       @RequestParam("pageNum") Integer pageNum,
                                                       @RequestParam("pageSize") Integer pageSize) {
        ResultJsonVO resultJsonVO = new ResultJsonVO(ResultJsonCode.REQUIRED_SUCCESS);
        Integer startIndex = (pageNum - 1) * pageSize;
        List<UserMessagePO> userMessagePOS = userMessageService.pagesUnreadByUsername(username, startIndex, pageSize);
        Integer count = userMessageService.selectCountUnreadByUsername(username);
        resultJsonVO.addInfo(userMessagePOS);
        resultJsonVO.addInfo(count);
        return resultJsonVO;
    }

    @PrivateRequired
    @GetMapping("/unread/count")
    public ResultJsonVO getUserUnreadMessageCountByUsername(@RequestParam("username") String username) {
        ResultJsonVO resultJsonVO = new ResultJsonVO(ResultJsonCode.REQUIRED_SUCCESS);
        Integer count = userMessageService.selectCountUnreadByUsername(username);
        resultJsonVO.addInfo(count);
        return resultJsonVO;
    }

    @PrivateRequired
    @PostMapping("/unread/set_read")
    public ResultJsonVO setUserMessageRead(@RequestParam("username") String username,
                                           @RequestParam("id") Integer id) {
        ResultJsonVO resultJsonVO = new ResultJsonVO();
        Integer integer = userMessageService.updateSetReadByUsernameAndId(username, id);
        if (integer == 1) {
            resultJsonVO.setStatus(ResultJsonCode.REQUIRED_SUCCESS);
        } else {
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "设置未读消息失败");
        }
        return resultJsonVO;
    }

    @PrivateRequired
    @PostMapping("/unread/set_all_read")
    public ResultJsonVO setUserMessageAllRead(@RequestParam("username") String username)
    {
        ResultJsonVO resultJsonVO = new ResultJsonVO();
        Integer integer = userMessageService.updateSetReadByUsername(username);
        if (integer >= 1) {
            resultJsonVO.addInfo(integer);
            resultJsonVO.setStatus(ResultJsonCode.REQUIRED_SUCCESS);
        } else {
            resultJsonVO.setStatus(ResultJsonCode.BUSINESS_FAIL, "设置未读消息失败");
        }
        return resultJsonVO;
    }


}
