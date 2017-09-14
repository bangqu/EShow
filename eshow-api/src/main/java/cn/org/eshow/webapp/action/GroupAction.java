package cn.org.eshow.webapp.action;

import cn.org.eshow.bean.query.GroupQuery;
import cn.org.eshow.common.page.Page;
import cn.org.eshow.component.easemob.api.impl.EasemobChatGroup;
import cn.org.eshow.model.Group;
import cn.org.eshow.model.User;
import cn.org.eshow.model.UserGroup;
import cn.org.eshow.service.AccessTokenManager;
import cn.org.eshow.service.GroupManager;
import cn.org.eshow.service.UserGroupManager;
import cn.org.eshow.util.DateUtil;
import cn.org.eshow.util.JacksonUtil;
import cn.org.eshow.webapp.action.response.GroupResponse;
import cn.org.eshow.webapp.util.RenderUtil;
import cn.org.eshow.webapp.util.Struts2Utils;
import io.swagger.client.model.ModifyGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.AllowedMethods;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 群组API接口
 */
@AllowedMethods({"search", "delete", "view", "update", "save", "recommend", "easemob"})
public class GroupAction extends ApiBaseAction<Group> {

    private static final long serialVersionUID = 1L;

    @Autowired
    private GroupManager groupManager;
    @Autowired
    private UserGroupManager userGroupManager;

    private Group group = new Group();
    private GroupQuery query = new GroupQuery(Boolean.TRUE);
    private List<Group> groups;

    private EasemobChatGroup easemobChatGroup = new EasemobChatGroup();

    private String userIds;//用户ids，逗号隔开
    private String easemobGroupIds;//群组环信ids

    /**
     * 搜索群组
     */
    public void search() {
        if (!StringUtils.isEmpty(easemobGroupIds)) {
            String easemobIds[] = easemobGroupIds.split(",");
            query.setEasemobGroupIds(easemobIds);
        }
        Page<Group> page = groupManager.search(query);
        if (page.getDataList().isEmpty()) {
            RenderUtil.failure("暂无群组列表");
            return;
        }
        query.setOrder("updateTime");
        query.setDesc(Boolean.TRUE);
        group = groupManager.search(query).getDataList().get(0);
        Long version = Long.valueOf(DateUtil.getDateTime("yyyyMMddHHmmss", group.getUpdateTime()));

        if (query.getVersion() != null && query.getVersion().equals(version)) {
            RenderUtil.failure("无需更新");
            return;
        }

        List<GroupResponse> responses = new ArrayList<GroupResponse>();
        for (Group obj : page.getDataList()) {
            responses.add(new GroupResponse(obj));
        }

        Struts2Utils.renderText("{\"status\":\"1\",\"msg\":\"" + "获取成功" + "\",\"version\":" + version + ",\"total\":" + page.getTotal() + ",\"totalPage\":" + page.getTotalPage() + ",\"pageSize\":" + page.getPageSize() + ",\"" + "groups" + "\":" + JacksonUtil.toJson(responses) + "}");
    }

    /**
     * 获取推荐群组
     */
    public void recommend() {
        query.setEnabled(true);
        query.setOrder("number");
        query.setDesc(Boolean.TRUE);
        Page<Group> page = groupManager.search(query);
        groups = page.getDataList();
        List<GroupResponse> responses = new ArrayList<GroupResponse>();
        for (Group obj : page.getDataList()) {
            responses.add(new GroupResponse(obj));
        }
        RenderUtil.page("获取成功", "groups", page, responses);
    }

    /**
     * 获取群组详情信息
     */
    public void view() {
        group = groupManager.get(id);
        if (group == null) {
            RenderUtil.failure("群组不存在");
            return;
        }
        Struts2Utils.renderText("{\"status\":\"1\",\"msg\":\"获取成功\",\"group\":" + JacksonUtil.toJson(new GroupResponse(group)) + "}");
    }

    /**
     * 创建一个群组
     */
    public void save() {
        User user = accessTokenManager.isValid(accessToken);
        if (user == null) {
            RenderUtil.expires();//用户信息过期
            return;
        }
        if (group.getOpen() == null) {
            RenderUtil.failure("非法参数");
            return;
        }
        if (StringUtils.isEmpty(userIds)) {
            RenderUtil.failure("请添加群成员");
            return;
        }

        List<User> users = new ArrayList<User>();

        Integer userSize = 0;
        String nicknames = "";
        String[] ids = userIds.split(",");
        String[] easemobIds = new String[ids.length];
        if (ids.length > 0) {
            Integer len = ids.length;
            for (int i = 0; i < len; i++) {
                User user1 = userManager.get(Integer.parseInt(ids[i]));
                if (user1 != null) {
                    String easemobId = user1.getUsername();
                    if (!StringUtils.isEmpty(easemobId)) {
                        easemobIds[i] = easemobId;
                        userSize += 1;
                        users.add(user1);
                        if (i < (len - 1)) {
                            nicknames += user1.getNickname() + "、";
                        } else {
                            nicknames += user1.getNickname();
                        }
                    }
                }
            }
        }
        if (StringUtils.isEmpty(group.getName())) {
            group.setName(nicknames);
        }
        if (StringUtils.isEmpty(group.getDescription())) {
            group.setDescription("群组");
        }
        query = new GroupQuery(Boolean.TRUE);
        query.setUserId(user.getId());
        query.setFullname(group.getName());
        Group old = groupManager.check(query);
        if (old != null) {
            if (old.getEnabled()) {
                Struts2Utils.renderText("{\"status\":\"0\",\"msg\":\"" + "您已创建该名称的群组" + "\",\"" + "group" + "\":" + JacksonUtil.toJson(new GroupResponse(old)) + "}");
                return;
            }
            old.setUpdateTime(new Date());
            old.setEnabled(Boolean.FALSE);
            old = groupManager.save(old);
            Struts2Utils.renderText("{\"status\":\"1\",\"msg\":\"创建成功\",\"group\":" + JacksonUtil.toJson(new GroupResponse(old)) + "}");
            return;
        }

        io.swagger.client.model.Group easemobGroup = new io.swagger.client.model.Group();
        easemobGroup.groupname(group.getName()).desc(group.getDescription())._public(true).maxusers(2000).approval(false).owner(group.getUser().getUsername());
        Object result = easemobChatGroup.createChatGroup(easemobGroup);

        String easemobGrouponId = "";//环信群组ID
        if (result == null) {
            RenderUtil.failure("创建失败");
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(result);
            if (jsonObject.has("data")) {
                JSONObject data = jsonObject.getJSONObject("data");
                if (data != null && data.has("groupid")) {
                    easemobGrouponId = data.optString("groupid");
                }
            }
        } catch (Exception e) {
            RenderUtil.failure("创建失败");
            return;
        }
        if (StringUtils.isEmpty(easemobGrouponId)) {
            RenderUtil.failure("创建失败");
            return;
        }
        if (easemobGroup.getMaxusers() == null) {
            group.setMaxNumber(2000);
        }
        group.setEasemobGroupId(easemobGrouponId);
        group.setNumber(userSize);
        group.setUser(user);
        group = groupManager.save(group);

        UserGroup ug = new UserGroup();
        ug.setAddTime(new Date());
        ug.setType(1);
        ug.setTop(Boolean.FALSE);
        if (group.getOpen()) {
            ug.setDisturb(Boolean.TRUE);
        }
        ug.setNickName(user.getNickname());
        ug.setGroup(group);
        ug.setUser(user);
        userGroupManager.save(ug);

        if (group != null && !users.isEmpty()) {
            for (User obj : users) {
                UserGroup userGroup = new UserGroup();
                userGroup.setAddTime(new Date());
                userGroup.setType(2);
                userGroup.setTop(Boolean.FALSE);
                if (group.getOpen()) {
                    userGroup.setDisturb(Boolean.TRUE);
                }
                userGroup.setNickName(obj.getNickname());
                userGroup.setGroup(group);
                userGroup.setUser(obj);
                userGroupManager.save(userGroup);
            }
        }

        Struts2Utils.renderText("{\"status\":\"1\",\"msg\":\"创建成功\",\"group\":" + JacksonUtil.toJson(new GroupResponse(group)) + "}");
    }

    /**
     * 更新群组信息
     */
    public void update() {
        User user = accessTokenManager.isValid(accessToken);
        if (user == null) {
            RenderUtil.expires();//用户信息过期
            return;
        }
        Group old = groupManager.get(id);
        if (old == null || old.getUser().getId() != user.getId()) {
            RenderUtil.failure("非法参数");
            return;
        }

        if (StringUtils.isEmpty(old.getEasemobGroupId())) {
            RenderUtil.failure("非法数据");
            return;
        }
        //修改名称、描述或者最大数量时再调用环信
        if ((group.getName() != null && !group.getName().equals(old.getName())) || (group.getDescription() != null && !group.getDescription().equals(old.getDescription())) || (group.getMaxNumber() != null && group.getMaxNumber() != old.getMaxNumber())) {
            String name = group.getName() != null ? group.getName() : old.getName();
            String desc = group.getDescription() != null ? group.getDescription() : old.getDescription();
            Integer maxNum = group.getMaxNumber() != null ? group.getMaxNumber() : old.getMaxNumber();

            ModifyGroup modifyGroup = new ModifyGroup();
            modifyGroup.description(desc).groupname(name).maxusers(maxNum);
            Object result = easemobChatGroup.modifyChatGroup(old.getEasemobGroupId(), modifyGroup);
            if (result == null) {
                RenderUtil.failure("修改失败");
                return;
            }
        }

        old.setName(group.getName() == null ? old.getName() : group.getName());
        old.setDescription(group.getDescription() == null ? old.getDescription() : group.getDescription());
        old.setImg(group.getImg() == null ? old.getImg() : group.getImg());
        old.setNumber(group.getNumber() == null ? old.getNumber() : group.getNumber());
        old.setMaxNumber(group.getMaxNumber() == null ? old.getMaxNumber() : group.getMaxNumber());

        group = groupManager.save(old);
        Struts2Utils.renderText("{\"status\":\"1\",\"msg\":\"修改成功\",\"group\":" + JacksonUtil.toJson(new GroupResponse(group)) + "}");
    }

    public void delete() {
        User user = accessTokenManager.isValid(accessToken);
        if (user == null) {
            RenderUtil.expires();//用户信息过期
            return;
        }
        Group old = groupManager.get(id);
        if (old == null || old.getUser().getId() != user.getId()) {
            RenderUtil.failure("非法参数");
            return;
        }
        old.setUpdateTime(new Date());
        old.setEnabled(Boolean.FALSE);

        if (StringUtils.isEmpty(old.getEasemobGroupId())) {
            RenderUtil.failure("非法数据");
            return;
        }

        Object result = easemobChatGroup.deleteChatGroup(old.getEasemobGroupId());
        if (result == null) {
            RenderUtil.failure("删除失败");
            return;
        }
        groupManager.save(old);
        RenderUtil.success("删除成功");

    }


    /**
     * 注册环信/根据环信获取用户信息
     */
    public void easemob() throws Exception {

        if (easemobGroupIds == null || accessToken == null) {
            RenderUtil.failure("非法参数");
            return;
        }
        User user = accessTokenManager.isValid(accessToken);
        if (user == null) {
            RenderUtil.expires();//用户信息过期
            return;
        }
        String[] easemobGroupIdList = StringUtils.split(easemobGroupIds, ",");
        List<GroupResponse> respones = new ArrayList<GroupResponse>();
        for (String easemobGroupId : easemobGroupIdList) {
            query = new GroupQuery(Boolean.TRUE);
            query.setEasemobGroupId(easemobGroupId);
            group = groupManager.check(query);
            if (group != null) {
                respones.add(new GroupResponse(group));
            }
        }
        if (!respones.isEmpty()) {
            Struts2Utils.renderText("{\"status\":\"1\",\"msg\":\"获取成功\",\"groups\":" + JacksonUtil.toJson(respones) + "}");
            return;
        }
        RenderUtil.failure("群不存在");
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public GroupQuery getQuery() {
        return query;
    }

    public void setQuery(GroupQuery query) {
        this.query = query;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public String getUserIds() {
        return userIds;
    }

    public void setUserIds(String userIds) {
        this.userIds = userIds;
    }

    public String getEasemobGroupIds() {
        return easemobGroupIds;
    }

    public void setEasemobGroupIds(String easemobGroupIds) {
        this.easemobGroupIds = easemobGroupIds;
    }
}