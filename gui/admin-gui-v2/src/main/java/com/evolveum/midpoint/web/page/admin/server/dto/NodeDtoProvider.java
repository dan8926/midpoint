/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterStatusInformation;
import com.evolveum.midpoint.task.api.Node;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class NodeDtoProvider extends BaseSortableDataProvider<NodeDto> {

    private static final transient Trace LOGGER = TraceManager.getTrace(NodeDtoProvider.class);
    private static final String DOT_CLASS = NodeDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_NODES = DOT_CLASS + "listNodes";
    private static final String OPERATION_COUNT_NODES = DOT_CLASS + "countNodes";

    private static final long ALLOWED_CLUSTER_INFO_AGE = 1200L;

    public NodeDtoProvider(PageBase page) {
        super(page);
    }

    @Override
    public Iterator<? extends NodeDto> iterator(int first, int count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_NODES);
        try {
            SortParam sortParam = getSort();
            OrderDirectionType order;
            if (sortParam.isAscending()) {
                order = OrderDirectionType.ASCENDING;
            } else {
                order = OrderDirectionType.DESCENDING;
            }

            PagingType paging = PagingTypeFactory.createPaging(first, count, order, sortParam.getProperty());

            TaskManager manager = getTaskManager();
            ClusterStatusInformation info = manager.getRunningTasksClusterwide(ALLOWED_CLUSTER_INFO_AGE, result);
            List<Node> nodes = manager.searchNodes(getQuery(), paging, info, result);

            for (Node node : nodes) {
                getAvailableData().add(new NodeDto(node));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unhandled exception when listing nodes", ex);
            result.recordFatalError("Couldn't list nodes.", ex);
        }

        return getAvailableData().iterator();
    }

    @Override
    public int size() {
        //todo fix error handling
        try {
            return getTaskManager().countNodes(getQuery(), new OperationResult(OPERATION_COUNT_NODES));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }
}
