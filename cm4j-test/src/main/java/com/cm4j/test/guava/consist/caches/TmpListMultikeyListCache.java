package com.cm4j.test.guava.consist.caches;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.consist.cc.ListReference;
import com.cm4j.test.guava.consist.entity.TmpListMultikey;
import com.cm4j.test.guava.consist.loader.CacheDefiniens;
import com.cm4j.test.guava.service.ServiceManager;

/**
 * COMMENT HERE
 *
 * User: yanghao
 * Date: 13-10-11 下午3:16
 */
public class TmpListMultikeyListCache extends CacheDefiniens<ListReference<TmpListMultikey>> {

    private int playerId;

    public TmpListMultikeyListCache(int playerId) {
        super(playerId);
        this.playerId = playerId;
    }

    @Override
    public ListReference<TmpListMultikey> load() {
        HibernateDao<TmpListMultikey, Integer> hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        hibernate.setPersistentClass(TmpListMultikey.class);
        String hql = "from TmpListMultikey where id.NPlayerId = ?";
        return new ListReference<TmpListMultikey>(hibernate.findAll(hql, playerId));
    }

    public TmpListMultikey findByType(int type) {
        ListReference<TmpListMultikey> ref = ref();

        for (TmpListMultikey _TmpListMultikey : ref.get()) {
            if (_TmpListMultikey.getId().getNType() == type) {
                return _TmpListMultikey;
            }
        }
        return null;
    }
}
