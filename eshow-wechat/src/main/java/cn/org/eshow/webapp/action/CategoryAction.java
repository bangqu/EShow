package cn.org.eshow.webapp.action;

import cn.org.eshow.bean.query.BlogQuery;
import cn.org.eshow.bean.query.CategoryQuery;
import cn.org.eshow.common.page.Page;
import cn.org.eshow.model.Blog;
import cn.org.eshow.model.Category;
import cn.org.eshow.service.BlogManager;
import cn.org.eshow.service.CategoryManager;
import cn.org.eshow.util.PageUtil;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Results({ @Result(name = "input", location = "add"),
		@Result(name = "list", type = "redirect", location = ""),
		@Result(name = "success", type = "redirect", location = "view/${id}"),
		@Result(name = "redirect", type = "redirect", location = "${redirect}") })
public class CategoryAction extends BaseAction {

	private static final long serialVersionUID = 1L;

	@Autowired
	private CategoryManager categoryManager;
	@Autowired
	private BlogManager blogManager;

	private List<Category> categories;
	private Category category;
	private CategoryQuery query = new CategoryQuery();

	public String list() {
		categories = categoryManager.list(query);
		return LIST;
	}

	public String search() {
		Page<Category> page = categoryManager.search(query);
		categories = page.getDataList();
		saveRequest("page", PageUtil.conversion(page));
		return LIST;
	}

	public String delete() {
		Category category = categoryManager.get(id);
		if (category != null) {
			// 查询当前分类下的日志
			BlogQuery blogQuery = new BlogQuery();
			blogQuery.setCategoryId(id);
			List<Blog> blogs = blogManager.list(blogQuery);
			for (Blog blog : blogs) {
				blog.setCategory(null);
				blogManager.save(blog);
			}
			categoryManager.remove(id);
			saveMessage("删除成功");

		}
		return LIST;
	}

	public String view() {
		if (id != null) {
			category = categoryManager.get(id);
		}
		return NONE;
	}

	public String update() throws Exception {
		Category old = categoryManager.get(id);
		old.setName(category.getName());
		old.setRemark(category.getRemark());
		categoryManager.save(old);
		saveMessage("修改成功");
		return LIST;
	}

	public String save() throws Exception {
		categoryManager.save(category);
		saveMessage("添加成功");
		return LIST;
	}

	public List<Category> getCategories() {
		return categories;
	}

	public void setCategories(List<Category> categories) {
		this.categories = categories;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public CategoryQuery getQuery() {
		return query;
	}

	public void setQuery(CategoryQuery query) {
		this.query = query;
	}

}