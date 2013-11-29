TaodianApi
==========

淘点开放API Java版SDK.


使用例子:

```java

TaodianApi api = new TaodianApi("77", "722cc7f45ed42ef51cf666b728a94812");

Map<String, Object> param = new HashMap<String, Object>();
param.put("short_key", shortKey);
param.put("auto_mobile", "y");

HTTPResult r = api.call("tool_convert_long_url", param);

if(r.isOK){
	String longUrl = r.getString("data.long_url");
} 
```
