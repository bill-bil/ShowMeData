package main;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.text.StringEscapeUtils;

/*
 * 所有配置的修改，界面的操作，都立即写入LineConfig对象，如有必要保存到磁盘，再调用一次SaveConfig函数，思路要清晰
 * 加载： 磁盘文件-->LineConfig对象--->具体控件的值
 * 保存： 具体各个控件的值---->LineConfig对象---->磁盘文件
 */

public class ToolPanel extends JPanel {

	private JLabel lblNewLabel_2;

	private boolean listenerIsOn = true;
	PrintWriter stdout;
	PrintWriter stderr;
	private JTextField BrowserPath;
	private JTextArea inputTextArea;
	private JTextArea outputTextArea;
	
	public boolean inputTextAreaChanged = true;
	public static JRadioButton showItemsInOne;
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JFrame frame = new JFrame();
					frame.setVisible(true);
					frame.setContentPane(new ToolPanel());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public ToolPanel() {
		setForeground(Color.DARK_GRAY);//构造函数
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));


			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
	


		///////////////////////HeaderPanel//////////////


		JPanel HeaderPanel = new JPanel();
		HeaderPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
		fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
		this.add(HeaderPanel, BorderLayout.NORTH);

		JLabel lblNewLabelNull = new JLabel("  ");
		HeaderPanel.add(lblNewLabelNull);

		//第一次分割，中间的大模块一分为二
		JSplitPane CenterSplitPane = new JSplitPane();//中间的大模块，一分为二
		CenterSplitPane.setResizeWeight(0.5);
		this.add(CenterSplitPane, BorderLayout.CENTER);

		//第二次分割：二分为四
		JSplitPane LeftOfCenter = new JSplitPane();
		LeftOfCenter.setResizeWeight(0.5);
		CenterSplitPane.setLeftComponent(LeftOfCenter);

		JSplitPane RightOfCenter = new JSplitPane();
		RightOfCenter.setOrientation(JSplitPane.VERTICAL_SPLIT);
		RightOfCenter.setResizeWeight(0.5);
		CenterSplitPane.setRightComponent(RightOfCenter);

		//  1/4
		JScrollPane oneFourthPanel = new JScrollPane();
		LeftOfCenter.setLeftComponent(oneFourthPanel);
		//  2/4
		JScrollPane twoFourthPanel = new JScrollPane();
		LeftOfCenter.setRightComponent(twoFourthPanel);

		inputTextArea = new JTextArea();
		inputTextArea.setColumns(20);
		inputTextArea.setLineWrap(true);
		inputTextArea.getDocument().addDocumentListener(new textAreaListener());
		oneFourthPanel.setViewportView(inputTextArea);

		outputTextArea = new JTextArea();
		outputTextArea.setLineWrap(true);
		twoFourthPanel.setViewportView(outputTextArea);


		//四分之三部分放一个panel，里面放操作按钮
		JPanel threeFourthPanel = new JPanel();
		RightOfCenter.setLeftComponent(threeFourthPanel);
		threeFourthPanel.setLayout(new FlowLayout());
		//https://stackoverflow.com/questions/5709690/how-do-i-make-this-flowlayout-wrap-within-its-jsplitpane
		threeFourthPanel.setMinimumSize(new Dimension(0, 0));//为了让button自动换行

		JButton btnOpenurls = new JButton("OpenURLs");
		threeFourthPanel.add(btnOpenurls);
		btnOpenurls.addActionListener(new ActionListener() {
			List<String> urls = new ArrayList<>();
			Iterator<String> it = urls.iterator();

			@Override
			public void actionPerformed(ActionEvent e) {
				if (inputTextAreaChanged) {//default is true
					urls = Arrays.asList(lineConfig.getToolPanelText().replaceAll(" ","").replaceAll("\r\n", "\n").split("\n"));
					it = urls.iterator();
					inputTextAreaChanged = false;
				}
				try {
					int i =10;
					while(i>0 && it.hasNext()) {
						String url = it.next();
						//stdout.println(url);
						Commons.browserOpen(url, lineConfig.getBrowserPath());
						i--;
					}
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}

		});

		JButton rows2List = new JButton("Rows To List");
		threeFourthPanel.add(rows2List);
		rows2List.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);					
					outputTextArea.setText(content.toString());
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		});

		JButton rows2Array = new JButton("Rows To Array");
		threeFourthPanel.add(rows2Array);
		rows2Array.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					for (int i=0;i<content.size();i++) {
						content.set(i, "\""+content.get(i)+"\"");
					}

					outputTextArea.setText(String.join(",", content));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		});


		JButton btnGrep = new JButton("Grep Json");
		threeFourthPanel.add(btnGrep);
		btnGrep.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String content = inputTextArea.getText();
					String toFind = JOptionPane.showInputDialog("to find which value", null);
					if (toFind == null) {
						return;
					} else {
						//stdout.println(content);
						ArrayList<String> result = JSONHandler.grepValueFromJson(content, toFind);
						//								stdout.println("##################Result of Grep JSON##################");
						//								stdout.println(result.toString());
						//								stdout.println("##################Result of Grep JSON##################");
						//								stdout.println();

						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}

				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					//e1.printStackTrace(stderr);
				}
			}

		});

		JButton btnLine = new JButton("Grep Line");
		threeFourthPanel.add(btnLine);
		btnLine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String toFind = JOptionPane.showInputDialog("to find which value", null);
					ArrayList<String> result = new ArrayList<String>();
					if (toFind == null) {
						return;
					} else {
						List<String> content = Commons.getLinesFromTextArea(inputTextArea);
						for (String item:content) {
							if (item.toLowerCase().contains(toFind.toLowerCase().trim())) {
								result.add(item); 
							}
						}
						//outputTextArea.setText(result.toString());
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}

				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					//e1.printStackTrace(stderr);
				}
			}
		});


		JButton btnAddPrefix = new JButton("Add Prefix/Suffix");
		threeFourthPanel.add(btnAddPrefix);
		btnAddPrefix.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String toAddPrefix = JOptionPane.showInputDialog("prefix to add", null);
					String toAddSuffix = JOptionPane.showInputDialog("suffix to add", null);
					ArrayList<String> result = new ArrayList<String>();
					if (toAddPrefix == null && toAddSuffix == null) {
						return;
					} else {
						if (toAddPrefix == null) {
							toAddPrefix = "";
						}
						
						if (toAddSuffix == null) {
							toAddSuffix = "";
						}
						
						List<String> content = Commons.getLinesFromTextArea(inputTextArea);
						for (String item:content) {
							item = toAddPrefix.trim()+item+toAddSuffix.trim();
							result.add(item); 
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		});
		

		JButton btnRegexGrep = new JButton("Regex Grep");
		btnRegexGrep.setEnabled(false);
		threeFourthPanel.add(btnRegexGrep);
		btnRegexGrep.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					//					String toFind = JOptionPane.showInputDialog("to find which value", null);
					//					if (toFind == null) {
					//						return;
					//					} else {
					ArrayList<String> result = new ArrayList<String>();
					//主要目的是找url        path: '/admin/menu',
					String webpack_PATTERN = "\'/([0-9a-z])*\'"; //TODO 正则表达不正确
					Pattern pRegex = Pattern.compile(webpack_PATTERN);
					String content = inputTextArea.getText();
					Matcher matcher = pRegex.matcher(content);
					while (matcher.find()) {//多次查找
						result.add(matcher.group());
					}
					outputTextArea.setText(result.toString());
					//}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		});

		JButton btnIPsToCIDR = new JButton("IPs To CIDR");
		threeFourthPanel.add(btnIPsToCIDR);
		btnIPsToCIDR.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> IPs = Commons.getLinesFromTextArea(inputTextArea);
					Set<String> subnets = Commons.toSmallerSubNets(new HashSet<String>(IPs));
					outputTextArea.setText(String.join(System.lineSeparator(), subnets));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});

		JButton btnCIDRToIPs = new JButton("CIDR To IPs");
		threeFourthPanel.add(btnCIDRToIPs);
		btnCIDRToIPs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<String> subnets = Commons.getLinesFromTextArea(inputTextArea);
					List<String> IPs = Commons.toIPList(subnets);// 当前所有title结果计算出的IP集合
					outputTextArea.setText(String.join(System.lineSeparator(), IPs));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});
		
		JButton unescapeJava = new JButton("unescapeJava");
		threeFourthPanel.add(unescapeJava);
		unescapeJava.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					outputTextArea.setText(StringEscapeUtils.unescapeJava(inputTextArea.getText()));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});
		
		JButton unescapeHTML = new JButton("unescapeHTML");
		threeFourthPanel.add(unescapeHTML);
		unescapeHTML.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					outputTextArea.setText(StringEscapeUtils.unescapeHtml4(inputTextArea.getText()));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		});

		JPanel fourFourthPanel = new JPanel();
		RightOfCenter.setRightComponent(fourFourthPanel);
		JLabel lblNewLabel = new JLabel("Browser Path:");
		fourFourthPanel.add(lblNewLabel);

		BrowserPath = new JTextField();
		fourFourthPanel.add(BrowserPath);
		BrowserPath.setColumns(50);
		BrowserPath.getDocument().addDocumentListener(new textFieldListener());

		///////////////////////////FooterPanel//////////////////


		JPanel footerPanel = new JPanel();
		footerPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		FlowLayout fl_FooterPanel = (FlowLayout) footerPanel.getLayout();
		fl_FooterPanel.setAlignment(FlowLayout.LEFT);
		this.add(footerPanel, BorderLayout.SOUTH);

		lblNewLabel_2 = new JLabel(BurpExtender.getExtenderName()+"    "+BurpExtender.getGithub());
		lblNewLabel_2.setFont(new Font("宋体", Font.BOLD, 12));
		lblNewLabel_2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					URI uri = new URI(BurpExtender.getGithub());
					Desktop desktop = Desktop.getDesktop();
					if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
						desktop.browse(uri);
					}
				} catch (Exception e2) {
					e2.printStackTrace(stderr);
				}

			}
			@Override
			public void mouseEntered(MouseEvent e) {
				lblNewLabel_2.setForeground(Color.BLUE);
			}
			@Override
			public void mouseExited(MouseEvent e) {
				lblNewLabel_2.setForeground(Color.BLACK);
			}
		});
		footerPanel.add(lblNewLabel_2);

	}

	public static Set<String> getSetFromTextArea(JTextArea textarea){
		//user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
		Set<String> domainList = new HashSet<>(Arrays.asList(textarea.getText().replaceAll(" ","").replaceAll("\r\n", "\n").split("\n")));
		domainList.remove("");
		return domainList;
	}


	class textAreaListener implements DocumentListener {

		@Override
		public void removeUpdate(DocumentEvent e) {
			lineConfig.setToolPanelText(inputTextArea.getText());
			inputTextAreaChanged = true;
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			lineConfig.setToolPanelText(inputTextArea.getText());
			inputTextAreaChanged = true;
		}

		@Override
		public void changedUpdate(DocumentEvent arg0) {
			lineConfig.setToolPanelText(inputTextArea.getText());
			inputTextAreaChanged = true;
		}
	}

	class textFieldListener implements DocumentListener {

		@Override
		public void removeUpdate(DocumentEvent e) {
			File browser = new File(BrowserPath.getText().trim());
			if (browser.exists()) {
				lineConfig.setBrowserPath(browser.getAbsolutePath());
				saveConfig();
			}
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			File browser = new File(BrowserPath.getText().trim());
			if (browser.exists()) {
				lineConfig.setBrowserPath(browser.getAbsolutePath());
				saveConfig();
			}
		}

		@Override
		public void changedUpdate(DocumentEvent arg0) {
			File browser = new File(BrowserPath.getText().trim());
			if (browser.exists()) {
				lineConfig.setBrowserPath(browser.getAbsolutePath());
				saveConfig();
			}
		}
	}
}
