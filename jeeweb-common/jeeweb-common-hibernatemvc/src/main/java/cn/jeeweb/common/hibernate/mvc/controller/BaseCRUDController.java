package cn.jeeweb.common.hibernate.mvc.controller;

import cn.jeeweb.common.http.PageResponse;
import cn.jeeweb.common.http.Response;
import cn.jeeweb.common.mvc.controller.BaseBeanController;
import cn.jeeweb.common.mvc.entity.AbstractEntity;
import cn.jeeweb.common.hibernate.mvc.service.ICommonService;
import cn.jeeweb.common.query.annotation.PageableDefaults;
import cn.jeeweb.common.query.data.PropertyPreFilterable;
import cn.jeeweb.common.query.data.Queryable;
import cn.jeeweb.common.security.shiro.authz.annotation.RequiresMethodPermissions;
import cn.jeeweb.common.utils.ObjectUtils;
import cn.jeeweb.common.utils.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeFilter;
import org.hibernate.criterion.DetachedCriteria;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public abstract class BaseCRUDController<Entity extends AbstractEntity<ID>, ID extends Serializable>
		extends BaseBeanController<Entity> {

	protected ICommonService<Entity> commonService;

	/**
	 * 设置基础service
	 *
	 * @param commonService
	 */
	@Autowired
	public void setCommonService(ICommonService<Entity> commonService) {
		this.commonService = commonService;
	}

	public Entity get(ID id) {
		if (!ObjectUtils.isNullOrEmpty(id)) {
			return commonService.get(id);
		} else {
			return newModel();
		}
	}

	/**
	 * list 运行之前
	 * 
	 * @param model
	 * @param request
	 * @param response
	 */
	public void preList(Model model, HttpServletRequest request, HttpServletResponse response) {
	}

	@RequiresMethodPermissions("list")
	@RequestMapping(method = RequestMethod.GET)
	public String list(Model model, HttpServletRequest request, HttpServletResponse response) {
		preList(model, request, response);
		return display("list");
	}

	/**
	 * 在异步获取数据之前
	 * 
	 * @param queryable
	 * @param detachedCriteria
	 * @param request
	 * @param response
	 */
	public void preAjaxList(Queryable queryable, DetachedCriteria detachedCriteria, HttpServletRequest request,
			HttpServletResponse response) {

	}

	/**
	 * 根据页码和每页记录数，以及查询条件动态加载数据
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping(value = "ajaxList", method = { RequestMethod.GET, RequestMethod.POST })
	@PageableDefaults(sort = "id=desc")
	private void ajaxList(Queryable queryable, PropertyPreFilterable propertyPreFilterable, HttpServletRequest request,
						  HttpServletResponse response) throws IOException {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(entityClass);
		preAjaxList(queryable, detachedCriteria, request, response);
		propertyPreFilterable.addQueryProperty("id");
		SerializeFilter filter = propertyPreFilterable.constructFilter(entityClass);
		PageResponse<Entity> pagejson = new PageResponse<Entity>(commonService.list(queryable, detachedCriteria));
		String content = JSON.toJSONString(pagejson, filter);
		StringUtils.printJson(response, content);
	}

	public String showView(Entity entity, Model model, HttpServletRequest request, HttpServletResponse response) {
		return "";
	}

	/**
	 * 
	 * @title: _view
	 * @description: 查看
	 * @param model
	 * @param id
	 * @param request
	 * @param response
	 * @return
	 * @return: String
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public String _view(Model model, @PathVariable("id") ID id, HttpServletRequest request,
                        HttpServletResponse response) {
		Entity entity = get(id);
		showUpdate(entity, model, request, response);
		model.addAttribute("data", entity);
		String showView = showView(newModel(), model, request, response);
		if (!StringUtils.isEmpty(showView)) {
			return display(showView);
		}
		return display("edit");
	}

	public void preEdit(Entity entity, Model model, HttpServletRequest request, HttpServletResponse response) {

	}

	public String showCreate(Entity entity, Model model, HttpServletRequest request, HttpServletResponse response) {
		return "";
	}

	@RequestMapping(value = "create", method = RequestMethod.GET)
	public String _showCreate(Model model, HttpServletRequest request, HttpServletResponse response) {
		preEdit(newModel(), model, request, response);
		String creteaView = showCreate(newModel(), model, request, response);
		if (!model.containsAttribute("data")) {
			model.addAttribute("data", newModel());
		}
		if (!StringUtils.isEmpty(creteaView)) {
			return creteaView;
		}
		return display("edit");
	}

	@RequestMapping(value = "create", method = RequestMethod.POST)
	@ResponseBody
	public Response create(Model model, @Valid @ModelAttribute("data") Entity entity, BindingResult result,
						   HttpServletRequest request, HttpServletResponse response) {
		return doSave(entity, request, response, result);
	}

	public String showUpdate(Entity entity, Model model, HttpServletRequest request, HttpServletResponse response) {
		return "";
	}

	@RequestMapping(value = "{id}/update", method = RequestMethod.GET)
	public String _showUpdate(@PathVariable("id") ID id, Model model, HttpServletRequest request,
                              HttpServletResponse response) {
		Entity entity = get(id);
		preEdit(entity, model, request, response);
		model.addAttribute("data", entity);
		String updateView = showUpdate(newModel(), model, request, response);
		if (!StringUtils.isEmpty(updateView)) {
			return updateView;
		}
		return display("edit");
	}

	@RequestMapping(value = "{id}/update", method = RequestMethod.POST)
	@ResponseBody
	public Response update(Model model, @Valid @ModelAttribute("data") Entity entity, BindingResult result,
                           HttpServletRequest request, HttpServletResponse response) {
		return doSave(entity, request, response, result);
	}

	/**
	 * 保存数据之前
	 * 
	 * @param entity
	 * @param request
	 * @param response
	 */
	public void preSave(Entity entity, HttpServletRequest request, HttpServletResponse response) {
	}

	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public Response doSave(Entity entity, HttpServletRequest request, HttpServletResponse response,
			BindingResult result) {
		if (hasError(entity, result)) {
			// 错误提示
			String errorMsg = errorMsg(result);
			if (!StringUtils.isEmpty(errorMsg)) {
				return Response.error(errorMsg);
			} else {
				return Response.error("保存失败");
			}
		}
		try {
			preSave(entity, request, response);
			if (ObjectUtils.isNullOrEmpty(entity.getId())) {
				commonService.save(entity);
			} else {
				// FORM NULL不更新
				Entity oldEntity = commonService.get(entity.getId());
				BeanUtils.copyProperties(entity,oldEntity);
				commonService.update(oldEntity);
			}
			afterSave(entity, request, response);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.error("保存失败!<br />原因:" + e.getMessage());
		}
		return Response.ok("保存成功");
	}

	/**
	 * 保存数据之后
	 * 
	 * @param entity
	 * @param request
	 * @param response
	 */
	public void afterSave(Entity entity, HttpServletRequest request, HttpServletResponse response) {
	}

	@RequestMapping(value = "{id}/delete", method = RequestMethod.POST)
	@ResponseBody
	public Response delete(@PathVariable("id") ID id) {
		commonService.deleteById(id);
		return Response.ok("删除成功");
	}

	@RequestMapping(value = "batch/delete", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public Response batchDelete(@RequestParam(value = "ids", required = false) ID[] ids) {
		List<ID> idList = java.util.Arrays.asList(ids);
		commonService.batchDeleteById(idList);
		return Response.ok("删除成功");
	}

}
