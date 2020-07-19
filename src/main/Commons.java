package main;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.ZoneTransferIn;

public class Commons {

	public static String set2string(Set<?> set){
		Iterator iter = set.iterator();
		StringBuilder result = new StringBuilder();
		while(iter.hasNext())
		{
			//System.out.println(iter.next());  		
			result.append(iter.next()).append("\n");
		}
		return result.toString();
	}

	public static boolean uselessExtension(String urlpath) {
		Set<String> extendset = new HashSet<String>();
		extendset.add(".gif");
		extendset.add(".jpg");
		extendset.add(".png");
		extendset.add(".css");
		Iterator<String> iter = extendset.iterator();
		while (iter.hasNext()) {
			if(urlpath.endsWith(iter.next().toString())) {//if no next(), this loop will not break out
				return true;
			}
		}
		return false;
	}



	public static boolean isValidIP (String ip) {
		if (ip.contains(":")) {//处理带有端口号的域名
			ip = ip.substring(0,ip.indexOf(":"));
		}
		
		try {
			if ( ip == null || ip.isEmpty() ) {
				return false;
			}

			String[] parts = ip.split( "\\." );
			if ( parts.length != 4 ) {
				return false;
			}

			for ( String s : parts ) {
				int i = Integer.parseInt( s );
				if ( (i < 0) || (i > 255) ) {
					return false;
				}
			}
			if ( ip.endsWith(".") ) {
				return false;
			}

			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	//http://www.xbill.org/dnsjava/dnsjava-current/examples.html
	public static HashMap<String,Set<String>> dnsquery(String domain) {
		HashMap<String,Set<String>> result = new HashMap<String,Set<String>>();
		try{
			Lookup lookup = new Lookup(domain, org.xbill.DNS.Type.A);
			lookup.run();

			Set<String> IPset = new HashSet<String>();
			Set<String> CDNSet = new HashSet<String>();
			if(lookup.getResult() == Lookup.SUCCESSFUL){
				Record[] records=lookup.getAnswers();
				for (int i = 0; i < records.length; i++) {
					ARecord a = (ARecord) records[i];
					String ip = a.getAddress().getHostAddress();
					String CName = a.getAddress().getHostName();
					if (ip!=null) {
						IPset.add(ip);
					}
					if (CName!=null) {
						CDNSet.add(CName);
					}
					//					System.out.println("getAddress "+ a.getAddress().getHostAddress());
					//					System.out.println("getAddress "+ a.getAddress().getHostName());
					//					System.out.println("getName "+ a.getName());
					//					System.out.println(a);
				}
				//				result.put("IP", IPset);
				//				result.put("CDN", CDNSet);
				//System.out.println(records);
			}
			result.put("IP", IPset);
			result.put("CDN", CDNSet);
			return result;

		}catch(Exception e){
			e.printStackTrace();
			return result;
		}
	}

	public static HashMap<String,Set<String>> dnsquery(String domain,String server) {
		HashMap<String,Set<String>> result = new HashMap<String,Set<String>>();
		try{
			Lookup lookup = new Lookup(domain, org.xbill.DNS.Type.A);
			Resolver resolver = new SimpleResolver(server);
			lookup.setResolver(resolver);
			lookup.run();

			Set<String> IPset = new HashSet<String>();
			Set<String> CDNSet = new HashSet<String>();
			if(lookup.getResult() == Lookup.SUCCESSFUL){
				Record[] records=lookup.getAnswers();
				for (int i = 0; i < records.length; i++) {
					ARecord a = (ARecord) records[i];
					String ip = a.getAddress().getHostAddress();
					String CName = a.getAddress().getHostName();
					if (ip!=null) {
						IPset.add(ip);
					}
					if (CName!=null) {
						CDNSet.add(CName);
					}
				}
			}
			result.put("IP", IPset);
			result.put("CDN", CDNSet);
			return result;

		}catch(Exception e){
			e.printStackTrace();
			return result;
		}
	}

	public static Set<String> GetAuthoritativeNameServer(String domain) {
		Set<String> NameServerSet = new HashSet<String>();
		try{
			Lookup lookup = new Lookup(domain, org.xbill.DNS.Type.NS);
			lookup.run();

			if(lookup.getResult() == Lookup.SUCCESSFUL){
				Record[] records=lookup.getAnswers();
				for (int i = 0; i < records.length; i++) {
					NSRecord a = (NSRecord) records[i];
					String server = a.getTarget().toString();
					if (server!=null) {
						NameServerSet.add(server);
					}
				}
			}
			return NameServerSet;

		}catch(Exception e){
			e.printStackTrace();
			return NameServerSet;
		}
	}
	
	public static List<String> ZoneTransferCheck(String domain,String NameServer) {
		List<String> Result = new ArrayList<String>();
		try {
			ZoneTransferIn zone = ZoneTransferIn.newAXFR(new Name(domain), NameServer, null);
			zone.run();
			Result = zone.getAXFR();
			System.out.print("!!! "+NameServer+" is zoneTransfer vulnerable for domain "+domain+" !");
		} catch (Exception e1) {
			System.out.print(String.format("[Server:%s Domain:%s] %s", NameServer,domain,e1.getMessage()));
		} 
		return Result;
	}
	

	//////////////////////////////////////////IP  subnet  CIDR/////////////////////////////////
	/*
	To Class C Network
	 */
	private static Set<String> toClassCSubNets(Set<String> IPSet) {
		Set<String> subNets= new HashSet<String>();
		Set<String> smallSubNets= new HashSet<String>();
		for (String ip:IPSet) {
			String subnet = ip.trim().substring(0,ip.lastIndexOf("."))+".0/24";
			subNets.add(subnet);
		}
		return subNets;
	}

	/*
	 * IP集合，转多个CIDR,smaller newtworks than Class C Networks
	 */
	public static Set<String> toSmallerSubNets(Set<String> IPSet) {
		Set<String> subNets= toClassCSubNets(IPSet);
		Set<String> smallSubNets= new HashSet<String>();

		for(String CNet:subNets) {//把所有IP按照C段进行分类
			SubnetUtils net = new SubnetUtils(CNet);
			Set<String> tmpIPSet = new HashSet<String>();
			for (String ip:IPSet) {
				if (net.getInfo().isInRange(ip) || net.getInfo().getBroadcastAddress().equals(ip.trim()) || net.getInfo().getNetworkAddress().equals(ip.trim())){
					//52.74.179.0 ---sometimes .0 address is a real address.
					tmpIPSet.add(ip);
				}
			}//每个tmpIPSet就是一个C段的IP集合
			smallSubNets.add(ipset2cidr(tmpIPSet));//把一个C段中的多个IP计算出其CIDR，即更小的网段
		}
		return smallSubNets;
	}
	/*
	To get a smaller network with a set of IP addresses
	 */
	private static String ipset2cidr(Set<String> IPSet) {
		if (IPSet == null || IPSet.size() <=0){
			return null;
		}
		if (IPSet.size() ==1){
			return IPSet.toArray(new String[0])[0];
		}
		Set<Long> tmp = new HashSet<Long>();
		List<String> list = new ArrayList<String>(IPSet);
		SubnetUtils oldsamllerNetwork =new SubnetUtils(list.get(0).trim()+"/24");
		for (int mask=24;mask<=32;mask++){
			//System.out.println(mask);
			SubnetUtils samllerNetwork = new SubnetUtils(list.get(0).trim()+"/"+mask);
			for (String ip:IPSet) {
				if (samllerNetwork.getInfo().isInRange(ip) || samllerNetwork.getInfo().getBroadcastAddress().equals(ip.trim()) || samllerNetwork.getInfo().getNetworkAddress().equals(ip.trim())){
					//52.74.179.0 ---sometimes .0 address is a real address.
					continue;
				}
				else {
					String networkaddress = oldsamllerNetwork.getInfo().getNetworkAddress();
					String tmpmask = oldsamllerNetwork.getInfo().getNetmask();
					return new SubnetUtils(networkaddress,tmpmask).getInfo().getCidrSignature();
				}
			}
			oldsamllerNetwork = samllerNetwork;
		}
		return null;
	}


	/*
	 * 多个网段转IP集合，变更表现形式，变成一个个的IP
	 */
	public static Set<String> toIPSet (Set<String> subNets) {
		Set<String> IPSet = new HashSet<String>();
		List<String> result = toIPList(new ArrayList<>(subNets));
		IPSet.addAll(result);
		return IPSet;
	}
	
	public static List<String> toIPList (List<String> subNets) {
		List<String> IPSet = new ArrayList<String>();
		for (String subnet:subNets) {
			try {
				if (subnet.contains(":")) {
					continue;//暂时先不处理IPv6,需要研究一下
					//TODO
				}
				if (subnet.contains("/")){
					SubnetUtils net = new SubnetUtils(subnet);
					SubnetInfo xx = net.getInfo();
					String[] ips = xx.getAllAddresses();
					IPSet.add(xx.getNetworkAddress());//.0
					IPSet.addAll(Arrays.asList(ips));
					IPSet.add(xx.getBroadcastAddress());//.255
				}else if (subnet.contains("-")) {
					String[] ips = subnet.split("-");
					if (ips.length ==2) {
						try {
							String startip = ips[0].trim();
							String endip = ips[1].trim();
							//System.out.println(startip);
							//System.out.println(endip);
							//Converts a String that represents an IP to an int.
							InetAddress i = InetAddress.getByName(startip);
							int startIPInt= ByteBuffer.wrap(i.getAddress()).getInt();

							if (endip.indexOf(".") == -1) {
								endip = startip.substring(0,startip.lastIndexOf("."))+endip;
								//System.out.println(endip);
							}
							InetAddress j = InetAddress.getByName(endip);
							int endIPInt= ByteBuffer.wrap(j.getAddress()).getInt();

							while (startIPInt <= endIPInt) {
								//System.out.println(startIPInt);
								startIPInt  = startIPInt+1;
								//This convert an int representation of ip back to String
								i= InetAddress.getByName(String.valueOf(startIPInt));
								String ip= i.getHostAddress();
								IPSet.add(ip);
								continue;
							}
							//System.out.print(IPSet);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}else { //单IP
					IPSet.add(subnet);
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		return IPSet;
	}

	public static String getNowTimeString() {
		SimpleDateFormat simpleDateFormat = 
				new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		return simpleDateFormat.format(new Date());
	}


	public static void browserOpen(Object url,String browser) throws Exception{
		String urlString = null;
		URI uri = null;
		if (url instanceof String) {
			urlString = (String) url;
			uri = new URI((String)url);
		}else if (url instanceof URL) {
			uri = ((URL)url).toURI();
			urlString = url.toString();
		}
		if(browser == null ||browser.equalsIgnoreCase("default") || browser.equalsIgnoreCase("")) {
			//whether null must be the first
			Desktop desktop = Desktop.getDesktop();
			if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
				desktop.browse(uri);
			}
		}else {
			String[] cmdArray = new String[] {browser,urlString};
			
			//runtime.exec(browser+" "+urlString);//当命令中有空格时会有问题
			Runtime.getRuntime().exec(cmdArray);
		}
	}

	public static List<Integer> Port_prompt(Component prompt, String str){
		String defaultPorts = "8080,8000,8443";
		String user_input = JOptionPane.showInputDialog(prompt, str,defaultPorts);
		if (null == user_input || user_input.trim().equals("")) return  null; 
		List<Integer> portList = new ArrayList<Integer>();
		for (String port: user_input.trim().split(",")) {
			int portint = Integer.parseInt(port);
			portList.add(portint);
		}
		return portList;
	}

	public static boolean isWindows() {
		String OS_NAME = System.getProperties().getProperty("os.name").toLowerCase();
		if (OS_NAME.contains("windows")) {
			return true;
		} else {
			return false;
		}
	}

	public static ArrayList<String> regexFind(String regex,String content) {
		ArrayList<String> result = new ArrayList<String>();
		Pattern pRegex = Pattern.compile(regex);
		Matcher matcher = pRegex.matcher(content);
		while (matcher.find()) {//多次查找
			result.add(matcher.group());
		}
		return result;
	}
	
	
	public static void writeToClipboard(String text) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection selection = new StringSelection(text);
		clipboard.setContents(selection, null);
	}
	
	public static void main(String args[]) {

		//		HashMap<String, Set<String>> result = dnsquery("www.baidu.com");
		//		System.out.println(result.get("IP").toString());
		//System.out.println(dnsquery("www.baidu111.com"));

		//		//System.out.println(new SubnetUtils("192.168.1.1/23").getInfo().getCidrSignature());
		//		
		//		Set<String> IPSet = new HashSet<String>();
		//		IPSet.add("192.168.1.225");
		///*		IPSet.add("192.168.1.128");
		//		IPSet.add("192.168.1.129");
		//		IPSet.add("192.168.1.155");
		//		IPSet.add("192.168.1.224");
		//		IPSet.add("192.168.1.130");*/
		//		Set<String> subnets = toSmallerSubNets(IPSet);
		//
		//		System.out.println(toIPSet(subnets));
		//		
		//		Set<String>  a= new HashSet();
		//		a.add("218.213.102.6/31");
		//		System.out.println(toIPSet(a));
//		Set<String> subnets = new HashSet<String>();
//		subnets.add("2402:db40:1::/48");
//		System.out.print(toIPSet(subnets));
		//System.out.print(dnsquery("0g.jd.com"));
		//System.out.print(GetAuthoritativeNameServer("jd.com"));
		ZoneTransferCheck("sf-express.com","ns4.sf-express.com");
	}
	
	/*
	 *将形如 https://www.runoob.com的URL统一转换为
	 * https://www.runoob.com:443/
	 * 
	 * 因为末尾的斜杠，影响URL类的equals的结果。
	 * 而默认端口影响String格式的对比结果。
	 */
	
	public static String formateURLString(String urlString) {
        try {
        	//urlString = "https://www.runoob.com";
			URL url = new URL(urlString);
			String host = url.getHost();
			int port = url.getPort();
			String path = url.getPath();
			
			if (port == -1) {
				String newHost = url.getHost()+":"+url.getDefaultPort();
				urlString = urlString.replace(host, newHost);
			}
			
			if (path.equals("")) {
				urlString = urlString+"/";
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return urlString;
	}
	
	public static List<String> getLinesFromTextArea(JTextArea textarea){
		//user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
		String[] lines = textarea.getText().replaceAll("\r\n", "\n").split("\n");
		List<String> result = new ArrayList<String>(Arrays.asList(lines));
		result.remove("");
		return result;
	}
}
