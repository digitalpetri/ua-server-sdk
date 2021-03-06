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

package com.digitalpetri.opcua.sdk.server.model.objects;

import java.util.Optional;

import com.digitalpetri.opcua.sdk.core.model.objects.TransitionEventType;
import com.digitalpetri.opcua.sdk.core.nodes.VariableNode;
import com.digitalpetri.opcua.sdk.server.api.UaNodeManager;
import com.digitalpetri.opcua.sdk.server.model.variables.StateVariableNode;
import com.digitalpetri.opcua.sdk.server.model.variables.TransitionVariableNode;
import com.digitalpetri.opcua.stack.core.types.builtin.DataValue;
import com.digitalpetri.opcua.stack.core.types.builtin.LocalizedText;
import com.digitalpetri.opcua.stack.core.types.builtin.NodeId;
import com.digitalpetri.opcua.stack.core.types.builtin.QualifiedName;
import com.digitalpetri.opcua.stack.core.types.builtin.Variant;
import com.digitalpetri.opcua.stack.core.types.builtin.unsigned.UByte;
import com.digitalpetri.opcua.stack.core.types.builtin.unsigned.UInteger;

@com.digitalpetri.opcua.sdk.server.util.UaObjectNode(typeName = "0:TransitionEventType")
public class TransitionEventNode extends BaseEventNode implements TransitionEventType {

    public TransitionEventNode(
            UaNodeManager nodeManager,
            NodeId nodeId,
            QualifiedName browseName,
            LocalizedText displayName,
            Optional<LocalizedText> description,
            Optional<UInteger> writeMask,
            Optional<UInteger> userWriteMask,
            UByte eventNotifier) {

        super(nodeManager, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier);
    }

    @Override
    public LocalizedText getTransition() {
        Optional<VariableNode> component = getVariableComponent("Transition");

        return component.map(node -> (LocalizedText) node.getValue().getValue().getValue()).orElse(null);
    }

    @Override
    public TransitionVariableNode getTransitionNode() {
        Optional<VariableNode> component = getVariableComponent("Transition");

        return component.map(node -> (TransitionVariableNode) node).orElse(null);
    }

    @Override
    public void setTransition(LocalizedText value) {
        getVariableComponent("Transition")
                .ifPresent(n -> n.setValue(new DataValue(new Variant(value))));
    }

    @Override
    public LocalizedText getFromState() {
        Optional<VariableNode> component = getVariableComponent("FromState");

        return component.map(node -> (LocalizedText) node.getValue().getValue().getValue()).orElse(null);
    }

    @Override
    public StateVariableNode getFromStateNode() {
        Optional<VariableNode> component = getVariableComponent("FromState");

        return component.map(node -> (StateVariableNode) node).orElse(null);
    }

    @Override
    public void setFromState(LocalizedText value) {
        getVariableComponent("FromState")
                .ifPresent(n -> n.setValue(new DataValue(new Variant(value))));
    }

    @Override
    public LocalizedText getToState() {
        Optional<VariableNode> component = getVariableComponent("ToState");

        return component.map(node -> (LocalizedText) node.getValue().getValue().getValue()).orElse(null);
    }

    @Override
    public StateVariableNode getToStateNode() {
        Optional<VariableNode> component = getVariableComponent("ToState");

        return component.map(node -> (StateVariableNode) node).orElse(null);
    }

    @Override
    public void setToState(LocalizedText value) {
        getVariableComponent("ToState")
                .ifPresent(n -> n.setValue(new DataValue(new Variant(value))));
    }

}
