package com.lwz.client.pool;

import java.util.List;

/**
 * @author liweizhou 2020/5/5
 */
public interface LoadBalance {

    ClientPool next(List<ClientPool> clientPoolList);

}
