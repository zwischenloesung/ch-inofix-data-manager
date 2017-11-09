<%--
    import/view.jsp: default view of the measurements import
    
    Created:    2017-11-01 18:58 by Christian Berndt
    Modified:   2017-11-01 18:58 by Christian Berndt
    Version:    1.0.0
--%>

<%@ include file="/init.jsp" %>

<%
    String displayStyle = ParamUtil.getString(request, "displayStyle", "list");
    long groupId = scopeGroupId;
    String navigation = ParamUtil.getString(request, "navigation", "all");
    String orderByCol = ParamUtil.getString(request, "orderByCol", "create-date");
    String orderByType = ParamUtil.getString(request, "orderByType", "desc");
    String searchContainerId = "importTaskRecordProcesses";
    tabs1 = ParamUtil.getString(request, "tabs1"); 
    tabs2 = ParamUtil.getString(request, "tabs2"); 

    PortletURL portletURL = renderResponse.createRenderURL();

    portletURL.setParameter("groupId", String.valueOf(groupId));
    portletURL.setParameter("displayStyle", displayStyle);
    portletURL.setParameter("mvcPath", "/import/view.jsp"); 
    portletURL.setParameter("navigation", navigation);
    portletURL.setParameter("orderByCol", orderByCol);
    portletURL.setParameter("orderByType", orderByType);
    portletURL.setParameter("searchContainerId", searchContainerId);
    portletURL.setParameter("tabs1", tabs1);
    portletURL.setParameter("tabs2", tabs2);

//     OrderByComparator<BackgroundTask> orderByComparator = BackgroundTaskComparatorFactoryUtil
//             .getBackgroundTaskOrderByComparator(orderByCol, orderByType);

//     int backgroundTasksCount = 0;
//     List<BackgroundTask> backgroundTasks = null;

    boolean completed = false;

    if ("completed".equals(navigation)) {
        completed = true;
    }

//     backgroundTasks = BackgroundTaskManagerUtil.getBackgroundTasks(scopeGroupId,
//             TaskRecordExportBackgroundTaskExecutor.class.getName(), 0, 20, orderByComparator);
//     backgroundTasksCount = BackgroundTaskManagerUtil.getBackgroundTasksCount(scopeGroupId,
//             TaskRecordExportBackgroundTaskExecutor.class.getName());
%>

<% // TODO: enable permission checks %>

<c:choose>
    <c:when test="<%= false %>">
<%--     <c:when test="<%= !GroupPermissionUtil.contains(permissionChecker, liveGroupId, ActionKeys.EXPORT_IMPORT_LAYOUTS) %>"> --%>
        <div class="alert alert-info">
            <liferay-ui:message key="you-do-not-have-permission-to-access-the-requested-resource" />
        </div>
    </c:when>
    <c:otherwise>
    
        <liferay-util:include page="/import/processes_list/view.jsp" servletContext="<%= application %>">
            <liferay-util:param name="displayStyle" value="<%= displayStyle %>" />
            <liferay-util:param name="navigation" value="<%= navigation %>" />
            <liferay-util:param name="orderByCol" value="<%= orderByCol %>" />
            <liferay-util:param name="orderByType" value="<%= orderByType %>" />
            <liferay-util:param name="searchContainerId" value="<%= searchContainerId %>" />
        </liferay-util:include>
        
        <liferay-util:include page="/import/add_button.jsp" servletContext="<%= application %>">
            <liferay-util:param name="groupId" value="<%= String.valueOf(groupId) %>" />
            <liferay-util:param name="displayStyle" value="<%= displayStyle %>" />
        </liferay-util:include>
        
<%--          
            <portlet:actionURL name="importMeasurements" var="importMeasurementsURL">
                <portlet:param name="<%= Constants.CMD %>" value="<%= Constants.IMPORT %>" />
                <portlet:param name="groupId" value="<%= String.valueOf(scopeGroupId) %>" />
            </portlet:actionURL>

            <aui:nav-item href="<%= importMeasurementsURL %>" iconCssClass="icon-arrow-down" label="import" />
--%>
    </c:otherwise>
</c:choose>