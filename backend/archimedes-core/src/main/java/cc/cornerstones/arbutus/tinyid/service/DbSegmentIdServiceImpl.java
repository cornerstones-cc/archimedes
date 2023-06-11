package cc.cornerstones.arbutus.tinyid.service;

import cc.cornerstones.arbutus.tinyid.share.types.TinyIdException;
import cc.cornerstones.arbutus.tinyid.share.constants.Constants;
import cc.cornerstones.arbutus.tinyid.persistence.TinyIdDao;
import cc.cornerstones.arbutus.tinyid.entity.TinyIdDo;
import cc.cornerstones.arbutus.tinyid.dto.SegmentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class DbSegmentIdServiceImpl implements SegmentIdService {

	private static final Logger logger = LoggerFactory.getLogger(DbSegmentIdServiceImpl.class);

	@Autowired
	private TinyIdDao tinyIdDao;

	/**
	 * Transactional 标记保证 query 和 update 使用的是同一连接
	 * 事务隔离级别应该为 READ_COMMITTED, Spring 默认是 DEFAULT (取决于底层使用的数据库，mysql 的默认隔离级别为 REPEATABLE_READ)
	 * <p>
	 * 如果是 REPEATABLE_READ，那么在本次事务中循环调用 tinyIdDao.queryByBizType(bizType) 获取的结果是没有变化的，也就是查询不到别的事务提交的内容
	 * 所以多次调用 tinyIdDao.updateMaxId 也就不会成功
	 *
	 * @param bizType
	 * @return
	 */
	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
	public SegmentId getNextSegmentId(String bizType) {
		logger.info("[tinyid] getNextSegmentId start::{}", bizType);

		// 获取nextTinyId的时候，有可能存在version冲突，需要重试
		for (int i = 0; i < Constants.RETRY; i++) {
			TinyIdDo tinyIdDo = tinyIdDao.queryByBizType(bizType);
			if (tinyIdDo == null) {
				throw new TinyIdException("[tinyid] cannot find biztype::" + bizType);
			}

			logger.info("[tinyid] getNextSegmentId retrying #{} {}", i, tinyIdDo);

			Long newMaxId = tinyIdDo.getMaxId() + tinyIdDo.getStep();
			Long oldMaxId = tinyIdDo.getMaxId();
			int row = tinyIdDao.updateMaxId(tinyIdDo.getId(), newMaxId, oldMaxId, tinyIdDo.getVersion(),
					tinyIdDo.getBizType());
			if (row == 1) {
				tinyIdDo.setMaxId(newMaxId);
				SegmentId segmentId = convert(tinyIdDo);
				logger.info("[tinyid] getNextSegmentId success::{}, current:{}", tinyIdDo, segmentId);
				return segmentId;
			} else {
				try {
					Thread.sleep(500L);
				} catch (InterruptedException e) {
					logger.error("", e);
				}
				logger.warn("[tinyid] getNextSegmentId conflict::{}", tinyIdDo);
			}
		}
		throw new TinyIdException("[tinyid] getNextSegmentId conflict::" + bizType);
	}

	public SegmentId convert(TinyIdDo idInfo) {
		SegmentId segmentId = new SegmentId();
		segmentId.setCurrentId(new AtomicLong(idInfo.getMaxId() - idInfo.getStep()));
		segmentId.setMaxId(idInfo.getMaxId());
		segmentId.setRemainder(idInfo.getRemainder() == null ? 0 : idInfo.getRemainder());
		segmentId.setDelta(idInfo.getDelta() == null ? 1 : idInfo.getDelta());
		// 默认20%加载
		segmentId.setLoadingId(segmentId.getCurrentId().get() + idInfo.getStep() * Constants.LOADING_PERCENT / 100);
		return segmentId;
	}
}
