package cn.jeeweb.generator.config;

import cn.jeeweb.common.http.Response;
import cn.jeeweb.common.utils.ServletUtils;
import org.apache.shiro.ShiroException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * All rights Reserved, Designed By www.jeeweb.cn
 *
 * @version V1.0
 * @package cn.gzst.gov.cloud.gateway.config
 * @title:
 * @description:自定义错误处理
 * @author: 王存见
 * @date: 2018/3/12 16:43
 * @copyright: 2017 www.jeeweb.cn Inc. All rights reserved.
 */
@RestControllerAdvice
public class ExceptionConfig {

    /**
     * 捕捉shiro的异常
     *
     * @param ex
     * @return
     */
    //@ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(ShiroException.class)
    public Object handle401(ShiroException ex) {
        if (ServletUtils.isAjax()) {
            return Response.error(401, ex.getMessage());
        }else{
            ModelAndView m = new ModelAndView();
            m.addObject("status", 401);
            m.setViewName("modules/front/other/404");
            return m;
        }
    }

    /**
     * 捕捉UnauthorizedException
     * @return
     */
    //@ResponseStatus(HttpStatus.UNAUTHORIZED)
    //@ExceptionHandler(UnauthorizedException.class)
    public Object handle401() {
        if (ServletUtils.isAjax()) {
            return Response.error(401, "Unauthorized");
        }else{
            ModelAndView m = new ModelAndView();
            m.addObject("status", 401);
            m.setViewName("modules/front/other/404");
            return m;
        }
    }

    /**
     * 捕捉其他所有异常
     *
     * @param request
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    //@ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object globalException(HttpServletRequest request, Throwable ex) {
        if (ServletUtils.isAjax()) {
            return Response.error(getStatus(request).value(), "页面或者数据被纸飞机运到火星了，啥都看不到了…");
        }else{
            ModelAndView m = new ModelAndView();
            m.addObject("status", getStatus(request).value());
            m.addObject("tips", ex.getMessage());
            m.setViewName("modules/front/other/404");
            return m;
        }
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.valueOf(statusCode);
    }
}
