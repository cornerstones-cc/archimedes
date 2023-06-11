package cc.cornerstones.biz.distributedjob.service.assembly;

import java.lang.reflect.Method;

public class JobBuiltinOperator extends JobOperator {
    private final Object underlyingObject;
    private final Method coreMethod;
    private Method initMethod;
    private Method destroyMethod;

    public JobBuiltinOperator(Object underlyingObject, Method coreMethod, Method initMethod, Method destroyMethod) {
        this.underlyingObject = underlyingObject;
        this.coreMethod = coreMethod;

        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    /**
     * execute, invoked when executor receives a scheduling request
     *
     * @throws Exception
     */
    @Override
    public void execute(Object... params) throws Exception {
        if (params != null && params.length > 0) {
            coreMethod.invoke(underlyingObject, params);
        } else {
            coreMethod.invoke(underlyingObject);
        }
    }

    /**
     * init, invoked when JobThread init
     */
    @Override
    public void init() throws Exception {
        if (initMethod != null) {
            initMethod.invoke(underlyingObject);
        }
    }

    /**
     * destroy, invoked when JobThread destroy
     */
    @Override
    public void destroy() throws Exception {
        if (destroyMethod != null) {
            destroyMethod.invoke(underlyingObject);
        }
    }

    @Override
    public String toString() {
        return "{" +
                "underlyingObject=" + underlyingObject +
                ", coreMethod=" + coreMethod +
                ", initMethod=" + initMethod +
                ", destroyMethod=" + destroyMethod +
                '}';
    }
}
