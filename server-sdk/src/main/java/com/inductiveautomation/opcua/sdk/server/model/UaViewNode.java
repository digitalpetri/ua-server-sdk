/*
 * Copyright 2014 Inductive Automation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inductiveautomation.opcua.sdk.server.model;

import java.util.Optional;

import com.inductiveautomation.opcua.sdk.core.AttributeIds;
import com.inductiveautomation.opcua.sdk.core.ValueRank;
import com.inductiveautomation.opcua.sdk.core.nodes.ViewNode;
import com.inductiveautomation.opcua.sdk.server.api.UaNamespace;
import com.inductiveautomation.opcua.sdk.server.model.Property.BasicProperty;
import com.inductiveautomation.opcua.stack.core.Identifiers;
import com.inductiveautomation.opcua.stack.core.types.builtin.LocalizedText;
import com.inductiveautomation.opcua.stack.core.types.builtin.NodeId;
import com.inductiveautomation.opcua.stack.core.types.builtin.QualifiedName;
import com.inductiveautomation.opcua.stack.core.types.builtin.unsigned.UByte;
import com.inductiveautomation.opcua.stack.core.types.builtin.unsigned.UInteger;
import com.inductiveautomation.opcua.stack.core.types.enumerated.NodeClass;

public class UaViewNode extends UaNode implements ViewNode {

    private volatile boolean containsNoLoops;
    private volatile UByte eventNotifier;

    public UaViewNode(UaNamespace nodeManager,
                      NodeId nodeId,
                      QualifiedName browseName,
                      LocalizedText displayName,
                      Optional<LocalizedText> description,
                      Optional<UInteger> writeMask,
                      Optional<UInteger> userWriteMask,
                      boolean containsNoLoops,
                      UByte eventNotifier) {

        super(nodeManager, nodeId, NodeClass.View, browseName, displayName, description, writeMask, userWriteMask);

        this.containsNoLoops = containsNoLoops;
        this.eventNotifier = eventNotifier;
    }

    @Override
    public Boolean getContainsNoLoops() {
        return containsNoLoops;
    }

    @Override
    public UByte getEventNotifier() {
        return eventNotifier;
    }

    @Override
    public synchronized void setContainsNoLoops(boolean containsNoLoops) {
        this.containsNoLoops = containsNoLoops;

        fireAttributeChanged(AttributeIds.ContainsNoLoops, containsNoLoops);
    }

    @Override
    public synchronized void setEventNotifier(UByte eventNotifier) {
        this.eventNotifier = eventNotifier;

        fireAttributeChanged(AttributeIds.EventNotifier, eventNotifier);
    }

    public static final Property<String> NodeVersion = new BasicProperty<>(
            new QualifiedName(0, "NodeVersion"),
            Identifiers.String,
            ValueRank.Scalar,
            String.class
    );

    public static final Property<UInteger> ViewVersion = new BasicProperty<>(
            new QualifiedName(0, "ViewVersion"),
            Identifiers.UInt32,
            ValueRank.Scalar,
            UInteger.class
    );

}
