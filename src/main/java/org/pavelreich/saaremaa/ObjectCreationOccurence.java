package org.pavelreich.saaremaa;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.reference.CtTypeReference;

/**
 * Created by preich on 19/02/19.
 */
class ObjectCreationOccurence {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectCreationOccurence.class);

    private CtElement element;
    private CtTypeReference typeRef;
    private InstanceType instanceType;

    public ObjectCreationOccurence(CtTypeReference mock, CtElement element, InstanceType instanceType) {
        this.typeRef = mock;
        this.element = element;
        this.instanceType = instanceType;
    }

    public String toCSV() {
        Integer line = null;
        String absolutePath = null;
        try {
            absolutePath = getAbsolutePath();
            line = typeRef.getPosition() instanceof NoSourcePosition ? null : typeRef.getPosition().getLine();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return instanceType + ";" + typeRef.toString() + ";" + absolutePath + ";" + line;
    }

    String getAbsolutePath() {
        try {
            return element.getPosition().getFile().getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public String getName() {
    	if (element instanceof CtInvocation || element instanceof CtConstructorCall) {
    		CtLocalVariable x = element.getParent(CtLocalVariable.class);
    		if (x != null) {
    			return x.getSimpleName();
    		}
    	}
    	if (element instanceof CtField) {
    		return ((CtField) element).getSimpleName();
    	}
    	return "unknown";
    }
    @Override
    public String toString() {
        return "[type=" + typeRef + ", element.position=" + element.getPosition() + "]";
    }

	public Map<String,Object> toJSON() {
		Map<String, Object> map = new HashMap();
		map.put("name", getName());
		map.put("class", typeRef != null && typeRef.getTypeDeclaration() != null ? typeRef.getTypeDeclaration().getQualifiedName() : "unknown");
		map.put("type", this.instanceType);
		return map;
	}
}
