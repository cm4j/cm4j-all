package com.cm4j.test.spring.extend_point.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;

public class AnimalEventListener implements ApplicationListener<AnimalSpeakEvent> {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private int counter;

	@Override
	public void onApplicationEvent(AnimalSpeakEvent animalSpeakEvent) {
		Animals animals = (Animals) animalSpeakEvent.getSource();

		logger.debug("=================");
		logger.debug("监听器执行次数：{}", ++counter);
		logger.debug("事件监听器{}:有一个动物在讲话！它的名字是:{}", this.getClass().getSimpleName(), animals.getAnimalName());
		Object[] args = animalSpeakEvent.getSelfDefinedArgs();
		if (args != null)
			logger.debug("获取到的自定义参数：{},{}", args[0], args[1]);
	}
}
