/*
 * digitalpetri OPC-UA SDK
 *
 * Copyright (C) 2015 Kevin Herron
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.digitalpetri.opcua.sdk.server.services.helpers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.digitalpetri.opcua.sdk.core.AttributeIds;
import com.digitalpetri.opcua.sdk.core.Reference;
import com.digitalpetri.opcua.sdk.server.DiagnosticsContext;
import com.digitalpetri.opcua.sdk.server.NamespaceManager;
import com.digitalpetri.opcua.sdk.server.OpcUaServer;
import com.digitalpetri.opcua.sdk.server.api.AttributeManager.ReadContext;
import com.digitalpetri.opcua.sdk.server.api.Namespace;
import com.digitalpetri.opcua.sdk.server.services.ServiceAttributes;
import com.digitalpetri.opcua.stack.core.StatusCodes;
import com.digitalpetri.opcua.stack.core.UaException;
import com.digitalpetri.opcua.stack.core.application.services.ServiceRequest;
import com.digitalpetri.opcua.stack.core.types.builtin.DataValue;
import com.digitalpetri.opcua.stack.core.types.builtin.DiagnosticInfo;
import com.digitalpetri.opcua.stack.core.types.builtin.ExpandedNodeId;
import com.digitalpetri.opcua.stack.core.types.builtin.NodeId;
import com.digitalpetri.opcua.stack.core.types.builtin.QualifiedName;
import com.digitalpetri.opcua.stack.core.types.builtin.StatusCode;
import com.digitalpetri.opcua.stack.core.types.enumerated.TimestampsToReturn;
import com.digitalpetri.opcua.stack.core.types.structured.BrowsePath;
import com.digitalpetri.opcua.stack.core.types.structured.BrowsePathResult;
import com.digitalpetri.opcua.stack.core.types.structured.BrowsePathTarget;
import com.digitalpetri.opcua.stack.core.types.structured.ReadValueId;
import com.digitalpetri.opcua.stack.core.types.structured.RelativePath;
import com.digitalpetri.opcua.stack.core.types.structured.RelativePathElement;
import com.digitalpetri.opcua.stack.core.types.structured.ResponseHeader;
import com.digitalpetri.opcua.stack.core.types.structured.TranslateBrowsePathsToNodeIdsRequest;
import com.digitalpetri.opcua.stack.core.types.structured.TranslateBrowsePathsToNodeIdsResponse;

import static com.digitalpetri.opcua.sdk.server.util.FutureUtils.sequence;
import static com.digitalpetri.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static com.digitalpetri.opcua.stack.core.util.ConversionUtil.a;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

public class BrowsePathsHelper {

    private final OpcUaServer server;
    private final NamespaceManager namespaceManager;

    public BrowsePathsHelper(OpcUaServer server, NamespaceManager namespaceManager) {
        this.server = server;
        this.namespaceManager = namespaceManager;
    }

    public void onTranslateBrowsePaths(
            ServiceRequest<TranslateBrowsePathsToNodeIdsRequest, TranslateBrowsePathsToNodeIdsResponse> service) {

        OpcUaServer server = service.attr(ServiceAttributes.SERVER_KEY).get();

        BrowsePath[] browsePaths = service.getRequest().getBrowsePaths();

        if (browsePaths.length >
                server.getConfig().getLimits().getMaxNodesPerTranslateBrowsePathsToNodeIds().intValue()) {

            service.setServiceFault(StatusCodes.Bad_TooManyOperations);
        } else {
            List<CompletableFuture<BrowsePathResult>> futures = newArrayListWithCapacity(browsePaths.length);

            for (BrowsePath browsePath : browsePaths) {
                futures.add(translate(browsePath));
            }

            sequence(futures).thenAcceptAsync(results -> {
                ResponseHeader header = service.createResponseHeader();
                TranslateBrowsePathsToNodeIdsResponse response = new TranslateBrowsePathsToNodeIdsResponse(
                        header, a(results, BrowsePathResult.class), new DiagnosticInfo[0]);

                service.setResponse(response);
            }, server.getExecutorService());
        }
    }

    private CompletableFuture<BrowsePathResult> translate(BrowsePath browsePath) {
        CompletableFuture<BrowsePathResult> future = new CompletableFuture<>();

        NodeId startingNode = browsePath.getStartingNode();
        RelativePath relativePath = browsePath.getRelativePath();

        follow(startingNode, newArrayList(relativePath.getElements())).whenComplete((targets, ex) -> {
            if (targets != null) {
                BrowsePathResult result;

                if (!targets.isEmpty()) {
                    result = new BrowsePathResult(
                            StatusCode.GOOD, a(targets, BrowsePathTarget.class));
                } else {
                    result = new BrowsePathResult(
                            new StatusCode(StatusCodes.Bad_NoMatch), new BrowsePathTarget[0]);
                }

                future.complete(result);
            } else {
                StatusCode statusCode = new StatusCode(StatusCodes.Bad_NoMatch);

                if (ex instanceof UaException) {
                    statusCode = ((UaException) ex).getStatusCode();
                }

                BrowsePathResult result = new BrowsePathResult(
                        statusCode, new BrowsePathTarget[0]);

                future.complete(result);
            }
        });

        return future;
    }

    private CompletableFuture<List<BrowsePathTarget>> follow(NodeId nodeId,
                                                             List<RelativePathElement> elements) {

        if (elements.size() == 1) {
            return target(nodeId, elements.get(0)).thenApply(targets ->
                    targets.stream()
                            .map(n -> new BrowsePathTarget(n, uint(0)))
                            .collect(toList()));
        } else {
            RelativePathElement e = elements.remove(0);

            return next(nodeId, e).thenCompose(nextExId -> {
                Optional<NodeId> nextId = namespaceManager.toNodeId(nextExId);

                if (nextId.isPresent()) {
                    return follow(nextId.get(), elements);
                } else {
                    List<BrowsePathTarget> targets = newArrayList(
                            new BrowsePathTarget(nextExId, uint(elements.size())));

                    return completedFuture(targets);
                }
            });
        }
    }

    private CompletableFuture<ExpandedNodeId> next(NodeId nodeId, RelativePathElement element) {
        NodeId referenceTypeId = element.getReferenceTypeId();
        boolean includeSubtypes = element.getIncludeSubtypes();
        QualifiedName targetName = element.getTargetName();

        Namespace namespace = namespaceManager.getNamespace(nodeId.getNamespaceIndex());

        CompletableFuture<List<Reference>> future = namespace.getReferences(nodeId);

        return future.thenCompose(references -> {
            List<ExpandedNodeId> targetNodeIds = references.stream()
                    /* Filter for references of the requested type or its subtype, if allowed... */
                    .filter(r -> referenceTypeId.isNull() ||
                            r.getReferenceTypeId().equals(referenceTypeId) ||
                            (includeSubtypes && r.subtypeOf(referenceTypeId, server.getReferenceTypes())))

                    /* Filter for reference direction... */
                    .filter(r -> r.isInverse() == element.getIsInverse())

                    /* Map to target ExpandedNodeId... */
                    .map(Reference::getTargetNodeId)
                    .collect(toList());

            return readTargetBrowseNames(targetNodeIds).thenApply(browseNames -> {
                for (int i = 0; i < targetNodeIds.size(); i++) {
                    ExpandedNodeId targetNodeId = targetNodeIds.get(i);
                    QualifiedName browseName = browseNames.get(i);
                    if (browseName.equals(targetName)) {
                        return targetNodeId;
                    }
                }

                return ExpandedNodeId.NULL_VALUE;
            });
        });
    }

    private CompletableFuture<List<ExpandedNodeId>> target(NodeId nodeId, RelativePathElement element) {
        NodeId referenceTypeId = element.getReferenceTypeId();
        boolean includeSubtypes = element.getIncludeSubtypes();
        QualifiedName targetName = element.getTargetName();

        Namespace namespace = namespaceManager.getNamespace(nodeId.getNamespaceIndex());

        CompletableFuture<List<Reference>> future = namespace.getReferences(nodeId);

        return future.thenCompose(references -> {
            List<ExpandedNodeId> targetNodeIds = references.stream()
                    /* Filter for references of the requested type or its subtype, if allowed... */
                    .filter(r -> referenceTypeId.isNull() ||
                            r.getReferenceTypeId().equals(referenceTypeId) ||
                            (includeSubtypes && r.subtypeOf(referenceTypeId, server.getReferenceTypes())))

                    /* Filter for reference direction... */
                    .filter(r -> r.isInverse() == element.getIsInverse())

                    /* Map to target ExpandedNodeId... */
                    .map(Reference::getTargetNodeId)
                    .collect(toList());

            return readTargetBrowseNames(targetNodeIds).thenApply(browseNames -> {
                List<ExpandedNodeId> targets = newArrayList();

                for (int i = 0; i < targetNodeIds.size(); i++) {
                    ExpandedNodeId targetNodeId = targetNodeIds.get(i);
                    QualifiedName browseName = browseNames.get(i);
                    if (matchesTarget(browseName, targetName)) {
                        targets.add(targetNodeId);
                    }
                }

                return targets;
            });
        });
    }

    private CompletableFuture<List<QualifiedName>> readTargetBrowseNames(List<ExpandedNodeId> targetNodeIds) {
        List<CompletableFuture<List<DataValue>>> futures = newArrayListWithCapacity(targetNodeIds.size());

        for (ExpandedNodeId xni : targetNodeIds) {
            CompletableFuture<List<DataValue>> future = xni.local().map(nodeId -> {
                Namespace namespace = namespaceManager.getNamespace(nodeId.getNamespaceIndex());

                ReadValueId readValueId = new ReadValueId(
                        nodeId, uint(AttributeIds.BrowseName), null, QualifiedName.NULL_VALUE);

                CompletableFuture<List<DataValue>> readFuture = new CompletableFuture<>();

                ReadContext context = new ReadContext(
                        server, null, readFuture, new DiagnosticsContext<>());

                namespace.read(context, 0.0, TimestampsToReturn.Neither, newArrayList(readValueId));

                return readFuture;
            }).orElse(completedFuture(newArrayList(new DataValue(StatusCodes.Bad_NodeIdUnknown))));

            futures.add(future);
        }

        return sequence(futures).thenApply(values ->
                values.stream().map(l -> {
                    DataValue v = l.get(0);
                    return (QualifiedName) v.getValue().getValue();
                }).collect(toList()));
    }

    private boolean matchesTarget(QualifiedName browseName, QualifiedName targetName) {
        return targetName == null ||
                targetName.equals(QualifiedName.NULL_VALUE) ||
                targetName.equals(browseName);
    }

}
