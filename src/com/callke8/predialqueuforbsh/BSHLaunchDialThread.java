package com.callke8.predialqueuforbsh;

import com.callke8.bsh.bshcallparam.BSHCallParamConfig;
import com.callke8.bsh.bshorderlist.BSHOrderList;
import com.callke8.system.param.ParamConfig;
import com.callke8.utils.BlankUtils;
import com.callke8.utils.DateFormatUtils;
import com.callke8.utils.StringUtil;

public class BSHLaunchDialThread implements Runnable {
	
	
	//private Log log = LogFactory.getLog(BSHLaunchDialThread.class);
	
	public BSHLaunchDialThread() {
		StringUtil.log(this, "线程BSHLaunchDialThread： 准备执行 ...");
	}

	@Override
	public void run() {
	
		try {
			Thread.sleep(5 * 1000);   //为了等待环境变量加载完毕,先休眠5秒
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		int i = 1;
		
		while(true) {
			
			//StringBuilder logSb = new StringBuilder();
			
			int activeChannelCount = BSHLaunchDialService.activeChannelCount;       //当前活动的通道数量
			int trunkMaxCapacity = Integer.valueOf(ParamConfig.paramConfigMap.get("paramType_3_trunkMaxCapacity"));		//中继的最大并发量
			
			if(BSHQueueMachineManager.queueCount > 0) {     //如果排队机中有未外呼的号码时
				
				//先判断中继最大并发量与当前活跃通话量对比,如果最大并发量大于当前活跃通话量时，表示还有空闲通道可用
				if(trunkMaxCapacity > activeChannelCount) {
					
					StringUtil.log(this, "(----外呼线程----)排队机中有未外呼数据:" + BSHQueueMachineManager.queueCount + " 条,系统将取出一条数据执行外呼!");
					
					BSHOrderList bshOrderList = BSHQueueMachineManager.deQueue();
					
					//在执行外呼之前，还需要增加一个判断，判断当前记录的外科状态是否已经被修改了（即是数据库中，是否还保持为1（已载入）状态）
					//为什么要加入这一判断：主要是因为对于状态为1（已载入）状态的数据，如果5分钟还没有得到空闲通道时，守护程序 BSHHandleState1TimerTask.java 
					//会强制将状态修改为3（待重呼）或是4（已失败）。如果此时仍执行外呼时，会造成数据不统一。固需要加一个判断，判断记录状态是否仍为1（已载入）
					
					boolean b = checkBshOrderListStateIs1(bshOrderList);
					
					if(b) {      //如果订单外呼状态仍为1，执行外呼
						
						Thread bshDialServiceThread = new Thread(new BSHLaunchDialService(bshOrderList));
						bshDialServiceThread.start();
						
					}else {      //如果订单外呼状态被修改了，则不执行外呼，并记录进日志
						StringUtil.log(this,"(----外呼线程----)数据处理：id: " + bshOrderList.getInt("ID") + ",订单ID：" + bshOrderList.get("ORDER_ID") + ",客户姓名: " + bshOrderList.get("CUSTOMER_NAME") + ", 客户号码: " + bshOrderList.get("CUSTOMER_TEL") + ",createTime: " + bshOrderList.getDate("CREATE_TIME").toString() + " 放弃执行外呼，属超时数据,已被强制处理!" );
						StringUtil.writeString("/opt/giveup_call_record.log", DateFormatUtils.getCurrentDate() + "\t 数据处理：id: " + bshOrderList.getInt("ID") + ",订单ID：" + bshOrderList.get("ORDER_ID") + ",客户姓名: " + bshOrderList.get("CUSTOMER_NAME") + ", 客户号码: " + bshOrderList.get("CUSTOMER_TEL") + ",createTime: " + bshOrderList.getDate("CREATE_TIME").toString() + " 放弃执行外呼，属超时数据,已被强制处理!\r\n", true);
					}
					
				}else {
					StringUtil.log(this, "(----外呼线程----)排队机中有未外呼数据:" + BSHQueueMachineManager.queueCount + " 条，但当前活跃通道已达到最大并发量：" + trunkMaxCapacity + "，系统暂不执行外呼!");
				}
			}else {
				StringUtil.log(this, "(----外呼线程----)当前排队机中没有未外呼数据,暂不执行外呼!");
			}
			
			i++;
			if(i > 10) {
				i = 1;
			}
			
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	/**
	 * 判断记录原状态是否为1，即是（已载入）
	 * 
	 * @param bshOrderList
	 * @return
	 */
	public boolean checkBshOrderListStateIs1(BSHOrderList bshOrderList) {
		
		boolean b = false;
		
		int id = bshOrderList.getInt("ID");   //取出ID
		
		//从数据库中取出订单记录
		BSHOrderList bol = BSHOrderList.dao.getBSHOrderListById(String.valueOf(id));
		
		//根据取出的 bol 判断是原状态是否仍为 1
		
		if(!BlankUtils.isBlank(bol)) {
			
			int state = bol.getInt("STATE");    //取出状态
			
			if(state == 1) {
				b = true;
			}
			
		}
		
		return b;
	}

}
