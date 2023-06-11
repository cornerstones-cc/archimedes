package cc.cornerstones.biz.share.assembly;

import java.util.List;

public abstract class ResourceReferenceOperator {

    /**
     * execute, invoked when executor receives a scheduling request
     *
     * @throws Exception
     */
    public abstract Object execute(Object... params) throws Exception;

    /**
     * init, invoked when JobThread init
     */
    public void init() throws Exception {
        // do something
    }


    /**
     * destroy, invoked when JobThread destroy
     */
    public void destroy() throws Exception {
        // do something
    }

}
