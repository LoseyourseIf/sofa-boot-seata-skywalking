package xingyu.lu.individual.svc.sofa.boot.facade;

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.filter.AutoActive;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;

@Extension(value = "transactionContextProvider")
@AutoActive(providerSide = true)
@Slf4j
public class SeataTxContextProviderFilter extends Filter{

    /**
     * log for this class
     */

    @Override
    public SofaResponse invoke(FilterInvoker filterInvoker, SofaRequest sofaRequest) throws SofaRpcException {
        String xid = RootContext.getXID();
        String rpcXid = getRpcXid(sofaRequest);
        if (log.isDebugEnabled()) {
            log.debug("xid in RootContext[" + xid + "] xid in RpcContext[" + rpcXid + "]");
        }
        boolean bind = false;
        if (xid != null) {
            RpcInternalContext.getContext().setAttachment(RootContext.KEY_XID, xid);
        } else {
            if (rpcXid != null) {
                RootContext.bind(rpcXid);
                bind = true;
                if (log.isDebugEnabled()) {
                    log.debug("bind[" + rpcXid + "] to RootContext");
                }
            }
        }
        try {
            return filterInvoker.invoke(sofaRequest);
        } finally {
            if (bind) {
                String unbindXid = RootContext.unbind();
                if (log.isDebugEnabled()) {
                    log.debug("unbind[" + unbindXid + "] from RootContext");
                }
                if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                    if (log.isWarnEnabled()) {
                        log.warn("xid in change during RPC from " + rpcXid + " to " + unbindXid);
                    }
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        if (log.isWarnEnabled()) {
                            log.warn("bind [" + unbindXid + "] back to RootContext");
                        }
                    }
                }
            }
        }
    }

    /**
     * get rpc xid
     * @return
     */
    private String getRpcXid(SofaRequest sofaRequest) {
        String rpcXid = (String) sofaRequest.getRequestProp(RootContext.KEY_XID);
        if (rpcXid == null) {
            rpcXid = (String) sofaRequest.getRequestProp(RootContext.KEY_XID.toLowerCase());
        }
        return rpcXid;
    }
}
