package org.pavelreich.saaremaa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;
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

    @Override
    public String toString() {
        return "[type=" + typeRef + ", element.position=" + element.getPosition() + "]";
    }
}
