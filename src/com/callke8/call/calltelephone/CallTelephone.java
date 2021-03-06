package com.callke8.call.calltelephone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.callke8.astutils.PhoneNumberHandlerUtils;
import com.callke8.utils.ArrayUtils;
import com.callke8.utils.BlankUtils;
import com.callke8.utils.DateFormatUtils;
import com.callke8.utils.MemoryVariableUtil;
import com.callke8.utils.NumberUtils;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
/**
 * CREATE TABLE `call_telephone` (
  `TEL_ID` bigint(32) NOT NULL AUTO_INCREMENT,
  `CT_ID` varchar(32) DEFAULT NULL,
  `TASK_NAME` varchar(32) DEFAULT NULL,
  `TELEPHONE` varchar(32) DEFAULT NULL,
  `CLIENT_NAME` varchar(255) DEFAULT NULL,
  `CLIENT_SEX` int(1) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `CREATE_USERCODE` varchar(32) DEFAULT NULL,
  `STATE` varchar(1) DEFAULT NULL,
  `OP_TIME` datetime DEFAULT NULL,
  `RETRY_TIMES` int(11) DEFAULT NULL,
  `IS_PUSH_KEY` varchar(1) DEFAULT NULL,
  `NEXT_CALLOUT_TIME` datetime DEFAULT NULL,
  `OPER_ID` varchar(255) DEFAULT NULL,
  `LOCATION` varchar(255) DEFAULT NULL,
  `VCHAR1` varchar(32) DEFAULT NULL,
  `VCHAR2` varchar(32) DEFAULT NULL,
  `VCHAR3` varchar(32) DEFAULT NULL,
  `VCHAR4` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`TEL_ID`)
)
 * @author Administrator
 *
 */
public class CallTelephone extends Model<CallTelephone> {
	
	public static CallTelephone dao = new CallTelephone();
	
	/**
	 * 新增外呼任务号码
	 * @param callTask
	 * @return
	 */
	public boolean add(Record telephone) {
		boolean b = Db.save("call_telephone","TEL_ID",telephone);
		return b;
	}
	
	/**
	 * 批量添加记录
	 * @param list
	 * @return
	 */
	public int add(ArrayList<Record> telephones) {
		int successCount = 0;
		for(Record telephone:telephones) {
			boolean b = add(telephone);
			if(b) {
				successCount++;
			}
		}
		return successCount;
		
		
	}
	
	/**
	 * 根据任务ID,删除号码
	 * @param taskId
	 * @return
	 */
	public int deleteByTaskId(int taskId){
		
		String sql = "delete from call_telephone where CT_ID=?";
		
		int count = Db.update(sql, taskId);
		
		return count;
	}
	
	/**
	 * 批量删除电话号码记录
	 * 
	 * @param ids
	 * 			ids 为电话号码的ID, 以逗号分隔
	 * @return
	 */
	public int batchDelete(String ids) {
		if(BlankUtils.isBlank(ids)) {    //如果传入的 ID 为空时，不作处理，并返回删除数量为 0
			return 0;
		}
		
		ArrayList<Record> list = new ArrayList<Record>();   //先创建一个record列表，主要是为了批量删除数据做准备
		
		String[] idList = ids.split(",");    //以逗号分隔ID
		
		for(String id:idList) {
			Record tel = new Record();
			tel.set("TEL_ID", id);
			list.add(tel);
		}
		
		String sql = "delete from call_telephone where TEL_ID=?";
		
		int[] delData = Db.batch(sql, "TEL_ID", list, 200);
		
		return delData.length;
	}
	
	/**
	 * 批量添加数据
	 * @param telephones
	 * @return
	 */
	public int batchSave(ArrayList<Record> telephones) {
		
		String sql = "insert into call_telephone(CT_ID,TELEPHONE,CLIENT_NAME,CLIENT_SEX,CREATE_TIME,CREATE_USERCODE,STATE) values(?,?,?,?,?,?,?)";

		int[] aaa = Db.batch(sql, "CT_ID,TELEPHONE,CLIENT_NAME,CLIENT_SEX,CREATE_TIME,CREATE_USERCODE,STATE", telephones, 5000);
		
		return aaa.length;   //返回插入成功的数
	}
	
	/**
	 * 回收已经分配的任务
	 * 		  即是在任务停止时，需要将已经分配到工号但未外呼的号码进行回收，可以使这些号码重新被利用
	 * 
	 * @param taskId
	 * @return
	 */
	public int reuse(int taskId) {
		
		String sql = "update call_telephone set STATE=?,OPER_ID=? where CT_ID=? and STATE=?";
		
		int count = Db.update(sql, "0","",taskId,"1");
		
		return count;
	}
	
	/**
	 * 根据条件，查询分页信息
	 * @param currentPage
	 * @param numPerPage
	 * @param ctId
	 * @param state
	 * @return
	 */
	public Page<Record> getCallTelephoneByPaginate(int currentPage,int numPerPage,String ctId,String telephone,String clientName,String state,String startTime,String endTime) {
		
		StringBuilder sb = new StringBuilder();
		Object[] pars = new Object[6];
		int index = 0;
		
		sb.append("from call_telephone where 1=1");
		
		if(!BlankUtils.isBlank(ctId)){
			sb.append(" and CT_ID=?");
			pars[index] = ctId;
			index++;
		}
		
		if(!BlankUtils.isBlank(telephone)) {
			sb.append(" and TELEPHONE like ?");
			pars[index] = "%" + telephone + "%";
			index++;
		}
		
		if(!BlankUtils.isBlank(clientName)) {
			sb.append(" and CLIENT_NAME like ?");
			pars[index] = "%" + clientName + "%";
			index++;
		}
		
		//if(!BlankUtils.isBlank(state) && !state.equalsIgnoreCase("4")) {
		if(!BlankUtils.isBlank(state)) {
			sb.append(" and STATE=?");
			pars[index] = state;
			index++;
		}
		
		if(!BlankUtils.isBlank(startTime)) {
			sb.append(" and CREATE_TIME>=?");
			pars[index] = startTime + " 00:00:00";
			index++;
		}
		
		if(!BlankUtils.isBlank(endTime)) {
			sb.append(" and CREATE_TIME<=?");
			pars[index] = endTime + "23:59:59";
			index++;
		}
		
		int s = (currentPage-1) * numPerPage;
		int e = s + numPerPage;
		
		System.out.println("sb.toString():" + sb.toString() + " ORDER BY TEL_ID DESC");
		
		Page<Record> page = Db.paginate(currentPage, numPerPage, "select *", sb.toString() + " ORDER BY TEL_ID DESC", ArrayUtils.copyArray(index, pars));
		return page;
	}
	
	
	
	/*public Map getCallTelephoneByPaginateToMap(int currentPage,int numPerPage,String ctId,String telephone,String clientName,String state,String startTime,String endTime) {
		long t1 = DateFormatUtils.getTimeMillis();
		System.out.println("查询分页前时间：" + t1);
		Page<Record> page = getCallTelephoneByPaginate(currentPage, numPerPage, ctId, telephone,clientName,state,startTime,endTime);
		long t2 = DateFormatUtils.getTimeMillis();
		System.out.println("查询分页 结束时间：" + t2);
		
		System.out.println("时间差为:" + (t2-t1));
		
		int total = page.getTotalRow();
		
		Map m = new HashMap();
		m.put("total", total);
		m.put("rows", page.getList());
		
		return m;
	}*/
	
	public Map getCallTelephoneByPaginateToMap(int currentPage,int numPerPage,String ctId,String telephone,String clientName,String state,String startTime,String endTime) {
		
		long t1 = DateFormatUtils.getTimeMillis();
		//System.out.println("查询数量前时间：" + t1);
		int total = getCallTelephoneCountByCondition(currentPage, numPerPage, ctId, telephone, clientName, state, startTime, endTime,null);
		long t2 = DateFormatUtils.getTimeMillis();
		
		System.out.println("查询数量时间为：" + (t2-t1));
		
		long t3 = DateFormatUtils.getTimeMillis();
		List<Record> list = customCallTelephoneByByPaginate(currentPage, numPerPage, ctId, telephone, clientName, state, startTime, endTime);
		
		List<Record> newList = new ArrayList<Record>();   //新建一个list，用于增加号码的状态描述
		
		for(Record record:list) {
			String telephoneState = record.get("STATE");    //先取出状态值
			
			record.set("STATE_DESC", MemoryVariableUtil.getDictName("CALL_STATE",telephoneState));
			newList.add(record);
		}
		
		
		long t4 = DateFormatUtils.getTimeMillis();
		System.out.println("查询记录时间为：" + (t4-t3));
		
		Map m = new HashMap();
		m.put("total", total);
		m.put("rows", newList);
		
		return m;
	}
	
	public Map getCallTelephoneByPaginateToMap4Auth(int currentPage,int numPerPage,String ctId,String telephone,String clientName,String state,String startTime,String endTime,String operId) {
		
		long t1 = DateFormatUtils.getTimeMillis();
		//System.out.println("查询数量前时间：" + t1);
		int total = getCallTelephoneCountByCondition(currentPage, numPerPage, ctId, telephone, clientName, state, startTime, endTime,operId);
		long t2 = DateFormatUtils.getTimeMillis();
		
		System.out.println("查询数量时间为：" + (t2-t1) + ",查出的数量为:" + total);
		
		long t3 = DateFormatUtils.getTimeMillis();
		List<Record> list = customCallTelephoneByByPaginate4Auth(currentPage, numPerPage, ctId, telephone, clientName, state, startTime, endTime,operId);
		
		List<Record> newList = new ArrayList<Record>();   //新建一个list，用于增加号码的状态描述
		
		for(Record record:list) {
			String telephoneState = record.get("STATE");    //先取出状态值
			
			record.set("STATE_DESC", MemoryVariableUtil.getDictName("CALL_STATE",telephoneState));
			newList.add(record);
		}
		
		
		long t4 = DateFormatUtils.getTimeMillis();
		System.out.println("查询记录时间为：" + (t4-t3));
		
		Map m = new HashMap();
		m.put("total", total);
		m.put("rows", newList);
		
		return m;
	}
	
	/**
	 * 自定义分页
	 * 
	 * @param currentPage
	 * @param numPerPage
	 * @param ctId
	 * @param telephone
	 * @param clientName
	 * @param state
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public List<Record> customCallTelephoneByByPaginate(int currentPage,int numPerPage,String ctId,String telephone,String clientName,String state,String startTime,String endTime) {
		
		StringBuilder sb = new StringBuilder();
		Object[] pars = new Object[6];
		int index = 0;
		
		sb.append("select TEL_ID from call_telephone where 1=1");
		
		if(!BlankUtils.isBlank(ctId)){
			sb.append(" and CT_ID=?");
			pars[index] = ctId;
			index++;
		}
		
		if(!BlankUtils.isBlank(telephone)) {
			sb.append(" and TELEPHONE like ?");
			pars[index] = "%" + telephone + "%";
			index++;
		}
		
		if(!BlankUtils.isBlank(clientName)) {
			sb.append(" and CLIENT_NAME like ?");
			pars[index] = "%" + clientName + "%";
			index++;
		}
		
		//if(!BlankUtils.isBlank(state) && !state.equalsIgnoreCase("4")) {
		if(!BlankUtils.isBlank(state)) {   //2016-11-2 hwz 修改
			sb.append(" and STATE=?");
			pars[index] = state;
			index++;
		}
		
		if(!BlankUtils.isBlank(startTime)) {
			sb.append(" and CREATE_TIME>=?");
			pars[index] = startTime + " 00:00:00";
			index++;
		}
		
		if(!BlankUtils.isBlank(endTime)) {
			sb.append(" and CREATE_TIME<=?");
			pars[index] = endTime + "23:59:59";
			index++;
		}
		
		sb.append(" order by TEL_ID desc");
		
		int s = (currentPage-1) * numPerPage;
		int e = s + numPerPage;
		
		sb.append(" limit " + s + "," + numPerPage);
		
		System.out.println(sb.toString());
		long t5 = DateFormatUtils.getTimeMillis();
		//这里查询出来的，只是TEL_ID的值，需要将这里的值取出
		List<Record> list = Db.find(sb.toString(), ArrayUtils.copyArray(index, pars));
		StringBuilder sbIds = new StringBuilder();  //拼凑 id
		for(Record r:list) {
			sbIds.append(r.get("TEL_ID") + ",");
		}
		long t6 = DateFormatUtils.getTimeMillis();
		
		System.out.println("查询ID出来的时间为:" + (t6-t5));
		long t7 = DateFormatUtils.getTimeMillis();
		String ids = sbIds.toString();
		if(!BlankUtils.isBlank(ids)) {    //如果ids 的结果不为空时，去掉最后一个逗号，并查询以 ids 为查询条件的 id 列表
			ids = ids.substring(0, ids.length()-1);
			
			String sql2 = "select * from call_telephone where TEL_ID in(" + ids + ") order by TEL_ID desc";
			
			List<Record> list2 = Db.find(sql2);
			long t8 = DateFormatUtils.getTimeMillis();
			System.out.println("查询记录出来的时候为:" + (t8-t7));
			return list2;
		}else {                         //如果结果为空时，直接返回 null
			return new ArrayList<Record>();
		}
	}
	
	/**
	 * 自定义分页
	 * 
	 * @param currentPage
	 * @param numPerPage
	 * @param ctId
	 * @param telephone
	 * @param clientName
	 * @param state
	 * @param startTime
	 * @param endTime
	 * @param operId
	 * @return
	 */
	public List<Record> customCallTelephoneByByPaginate4Auth(int currentPage,int numPerPage,String ctId,String telephone,String clientName,String state,String startTime,String endTime,String operId) {
		
		StringBuilder sb = new StringBuilder();
		Object[] pars = new Object[7];
		int index = 0;
		
		sb.append("select TEL_ID from call_telephone where 1=1");
		
		if(!BlankUtils.isBlank(ctId)){
			sb.append(" and CT_ID=?");
			pars[index] = ctId;
			index++;
		}
		
		if(!BlankUtils.isBlank(telephone)) {
			sb.append(" and TELEPHONE like ?");
			pars[index] = "%" + telephone + "%";
			index++;
		}
		
		if(!BlankUtils.isBlank(clientName)) {
			sb.append(" and CLIENT_NAME like ?");
			pars[index] = "%" + clientName + "%";
			index++;
		}
		
		//if(!BlankUtils.isBlank(state) && !state.equalsIgnoreCase("4")) {
		if(!BlankUtils.isBlank(state)) {   //2016-11-2 hwz修改
			sb.append(" and STATE=?");
			pars[index] = state;
			index++;
		}
		
		if(!BlankUtils.isBlank(startTime)) {
			sb.append(" and CREATE_TIME>=?");
			pars[index] = startTime + " 00:00:00";
			index++;
		}
		
		if(!BlankUtils.isBlank(endTime)) {
			sb.append(" and CREATE_TIME<=?");
			pars[index] = endTime + "23:59:59";
			index++;
		}
		
		if(!BlankUtils.isBlank(operId)) {
			sb.append(" and OPER_ID=?");
			pars[index] = operId;
			index++;
		}
		
		sb.append(" order by TEL_ID desc");
		
		int s = (currentPage-1) * numPerPage;
		int e = s + numPerPage;
		
		sb.append(" limit " + s + "," + numPerPage);
		
		System.out.println(sb.toString());
		long t5 = DateFormatUtils.getTimeMillis();
		//这里查询出来的，只是TEL_ID的值，需要将这里的值取出
		List<Record> list = Db.find(sb.toString(), ArrayUtils.copyArray(index, pars));
		StringBuilder sbIds = new StringBuilder();  //拼凑 id
		for(Record r:list) {
			sbIds.append(r.get("TEL_ID") + ",");
		}
		long t6 = DateFormatUtils.getTimeMillis();
		
		System.out.println("查询ID出来的时间为:" + (t6-t5));
		long t7 = DateFormatUtils.getTimeMillis();
		String ids = sbIds.toString();
		if(!BlankUtils.isBlank(ids)) {    //如果ids 的结果不为空时，去掉最后一个逗号，并查询以 ids 为查询条件的 id 列表
			ids = ids.substring(0, ids.length()-1);
			
			String sql2 = "select * from call_telephone where TEL_ID in(" + ids + ") order by TEL_ID desc";
			
			List<Record> list2 = Db.find(sql2);
			long t8 = DateFormatUtils.getTimeMillis();
			System.out.println("查询记录出来的时候为:" + (t8-t7));
			return list2;
		}else {                         //如果结果为空时，直接返回 null
			return new ArrayList<Record>();
		}
	}
	
	
	/**
	 * 自定义分页
	 * 
	 * @param currentPage
	 * @param numPerPage
	 * @param ctId
	 * @param telephone
	 * @param clientName
	 * @param state
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public int getCallTelephoneCountByCondition(int currentPage,int numPerPage,String ctId,String telephone,String clientName,String state,String startTime,String endTime,String operId) {
		
		StringBuilder sb = new StringBuilder();
		Object[] pars = new Object[7];
		int index = 0;
		
		sb.append("select count(*) as count from call_telephone where 1=1");
		
		if(!BlankUtils.isBlank(ctId)){
			sb.append(" and CT_ID=?");
			pars[index] = ctId;
			index++;
		}
		
		if(!BlankUtils.isBlank(telephone)) {
			sb.append(" and TELEPHONE like ?");
			pars[index] = "%" + telephone + "%";
			index++;
		}
		
		if(!BlankUtils.isBlank(clientName)) {
			sb.append(" and CLIENT_NAME like ?");
			pars[index] = "%" + clientName + "%";
			index++;
		}
		
		//if(!BlankUtils.isBlank(state) && !state.equalsIgnoreCase("4")) {
		if(!BlankUtils.isBlank(state)) {   //2016-11-2 hwz 修改
			sb.append(" and STATE=?");
			pars[index] = state;
			index++;
		}
		
		if(!BlankUtils.isBlank(startTime)) {
			sb.append(" and CREATE_TIME>=?");
			pars[index] = startTime + " 00:00:00";
			index++;
		}
		
		if(!BlankUtils.isBlank(endTime)) {
			sb.append(" and CREATE_TIME<=?");
			pars[index] = endTime + "23:59:59";
			index++;
		}
		
		if(!BlankUtils.isBlank(operId)) {
			sb.append(" and OPER_ID=?");
			pars[index] = operId;
			index++;
		}
		
		int s = (currentPage-1) * numPerPage;
		int e = s + numPerPage;
		
		System.out.println(sb.toString());
		System.out.println("-------------数据访问SQL111：" + sb.toString());
		Record record = Db.findFirst(sb.toString(), ArrayUtils.copyArray(index, pars));
		return Integer.valueOf(record.get("count").toString());
	}
	
	/**
	 * 请求数据
	 * 
	 * @param taskId
	 * 			 外呼任务ID
	 * @param operId
	 * 			 当前登录的工号
	 * @param count
	 * 		     请求的数据量
	 * @return
	 */
	public int reqCallData(int taskId,String operId,int count) {
		
		//定义一个SQL，用于先查询 TEL_ID，再根据这个 TEL_ID 去查询号码记录，查找 任务的ID状态为 0 ，即是新号码时
		//注：这里不使用 select *  from，主要是查询 TEL_ID 在效率上肯定会比这个效率快
		String sql = "select TEL_ID from call_telephone where CT_ID=? and STATE=0 order by TEL_ID DESC limit ?";
		
		List<Record> listId = Db.find(sql, taskId,count);   //查询出ID列表
		String ids = "";   //定义一个 ids
		for(Record r:listId) {
			ids += r.get("TEL_ID") + ",";
		}
		
		List<Record> list = new ArrayList<Record>();     //先定义一个 list
		if(!BlankUtils.isBlank(ids)) {
			ids = ids.substring(0, ids.length()-1);  //去掉最后的逗号
			String sql2 = "select * from call_telephone where TEL_ID in(" + ids + ")";
			
			List<Record> listRs = Db.find(sql2);                   //真实查询到数据
			
			for(Record r:listRs) {                         //要根据手机号码，新增一个字段，用于查询归属地
				String tel = r.get("TELEPHONE");
				
				r.set("OPER_ID",operId);
				
				String location = r.get("LOCATION");   //先取出归属地，如果为空时，才重新设置其归属地
				if(BlankUtils.isBlank(location)) {     //如果为空时，才设置其归属地
					r.set("LOCATION",PhoneNumberHandlerUtils.getLocation(tel));
				}
				list.add(r);
			}
			
			//更改号码的状态
			return batchUpdateTelephoneState(list);   //返回请求到的数据数量
		}
		
		return 0;
	}
	
	/**
	 * 单条更改号码的状态，主要是用于呼叫成功，或是呼叫失败时
	 * 
	 * @param TEL_ID
	 * @param state
	 */
	public boolean updateTelephoneState(String telId,String state) {
		boolean b = false;
		String sql = "update call_telephone set STATE=? where TEL_ID=?";
		int count = Db.update(sql,telId,state);
		
		if(count>0) {
			b = true;
		}
		return b;
	}
	
	/**
	 * 批量更改号码的状态，主要是用于操作员请求数据时，批量更改
	 * 
	 * @param list
	 * @return 更改成功的数量
	 */
	public int batchUpdateTelephoneState(List<Record> list) {
		
		String sql = "update call_telephone set STATE=1,OPER_ID=?,LOCATION=? where TEL_ID=?";
		
		int[] count = Db.batch(sql, "OPER_ID,LOCATION,TEL_ID", list, 10);
		
		return count.length;
	}
	
	
	/**
	 * 根据任务Id及号码的状态，返回数量
	 * 
	 * @param taskId
	 * @param state
	 * @return
	 */
	public int getCountByTaskIdState(int taskId,String state) {
		
		String sql = "select count(*) as count from call_telephone where CT_ID=? and STATE=?";
		
		Record record = Db.findFirst(sql, taskId,state);
		
		return Integer.valueOf(record.get("count").toString());
	}
	
	/**
	 * 根据 telId 查询号码信息
	 * @param telId
	 * @return
	 */
	public Record getCallTelephoneById(String telId){
		
		String sql = "select * from call_telephone where TEL_ID=? limit 1";
		
		Record record = Db.findFirst(sql,telId);
		
		return record;
	}
	
	
	/**
	 * 更新callTelephone的对象，主要是用于外呼结束后，更改一些相关的信息，如：状态，通话的备注，下次外呼的时间等等
	 * 
	 * @param callTelephone
	 * @return
	 */
	public boolean updateCallTelephone(CallTelephone callTelephone) {
		boolean b = false;
		
		int telId = Integer.valueOf(callTelephone.get("TEL_ID").toString());
		String clientName = callTelephone.get("CLIENT_NAME");
		String clientSex = callTelephone.get("CLIENT_SEX");
		String state = callTelephone.get("STATE");
		String nextCalloutTime = callTelephone.get("NEXT_CALLOUT_TIME");
		String opTime = callTelephone.get("OP_TIME");
		String operId = callTelephone.get("OPER_ID");
		String note = callTelephone.get("NOTE");
		
		StringBuilder sb = new StringBuilder();
		Object[] pars = new Object[10];
		int index = 0;
		
		sb.append("update call_telephone set ");
		
		if(!BlankUtils.isBlank(clientName)) {
			sb.append("CLIENT_NAME=?");
			pars[index] = clientName;
			//pars[index] = "111";
			index++;
		}
		if(!BlankUtils.isBlank(clientSex)) {
			sb.append(",CLIENT_SEX=?");
			pars[index] = clientSex;
			index++;
		}
		if(!BlankUtils.isBlank(state)) {
			sb.append(",STATE=?");
			pars[index] = state;
			index++;
		}
		if(!BlankUtils.isBlank(nextCalloutTime)) {
			sb.append(",NEXT_CALLOUT_TIME=?");
			pars[index] = nextCalloutTime;
			index++;
		}
		if(!BlankUtils.isBlank(opTime)) {
			sb.append(",OP_TIME=?");
			pars[index] = opTime;
			index++;
		}
		if(!BlankUtils.isBlank(operId)) {
			sb.append(",OPER_ID=?");
			pars[index] = operId;
			index++;
		}
		if(!BlankUtils.isBlank(note)) {
			sb.append(",NOTE=?");
			pars[index] = note;
			index++;
		}
		
		sb.append(" where TEL_ID=?");
		pars[index] = telId;
		index++;
		
		System.out.println("修改的語句：" + sb.toString());
		
		int count = Db.update(sb.toString(),ArrayUtils.copyArray(index, pars));
		
		if(count>0) {
			b = true;
		}
		
		return b;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
