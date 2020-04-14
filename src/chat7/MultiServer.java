package chat7;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiServer {
	
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	//클라이언트 정보 저장을 위한 Map컬렉션 정의
	Map<String, PrintWriter> clientMap;
	
	PreparedStatement psmt;
	Connection con;
	ResultSet rs;
	
	//생성자
	public MultiServer() {
		//클라이언트의 이름과 출력스트림을 저장할 HashMap생성
		clientMap = new HashMap<String, PrintWriter>();
		//HashMap동기화 설정. 쓰레드가 사용자정보에 동시에 접근하는 것을 차단한다.
		Collections.synchronizedMap(clientMap);
		
	}
	
	public void init() {
		
		try {
			
			//데이터베이스 연결
			try {
				Class.forName("oracle.jdbc.OracleDriver");
				con = DriverManager.getConnection
						("jdbc:oracle:thin://@localhost:1521:orcl", 
								"kosmo","1234"
						);
				System.out.println("오라클 DB 연결성공");

				
			}
			catch (ClassNotFoundException e) {
				System.out.println("오라클 드라이버 로딩 실패");
				e.printStackTrace();
			}
			catch (SQLException e) {
				System.out.println("DB 연결 실패");
				e.printStackTrace();
			}
			catch (Exception e) {
				System.out.println("알수 없는 예외 발생");
			}
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			
			while (true) {
				socket = serverSocket.accept();
				System.out.println(socket.getInetAddress());
				/*
				클라이언트의 메세지를 모든 클라이언트에게 전달하기 위한
				쓰레드 생성 및 start.
				 */
				Thread mst = new MultiServerT(socket);
				mst.start();
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		MultiServer ms = new MultiServer();
		ms.init();
	}
	
	//접속된 모든 클라이언트에게 메세지를 전달하는 역할의 메소드
	public void sendAllMsg(String name, String msg) {
		
		//Map에 저장된 객체의 키값(이름)을 먼저 얻어온다.
		Iterator<String> it = clientMap.keySet().iterator();
		
		//저장된 객체(클라이언트)의 갯수만큼 반복한다.
		while (it.hasNext()) {
			String user = it.next();
			try {
				//각 클라이언트의 PrintWriter객체를 얻어온다.
				PrintWriter it_out = (PrintWriter)
								clientMap.get(user);
				
				//클라이언트에게 메세지를 전달한다.
				/*
				매개변수 name이 있는 경우에는 이름 + 메세지
				없는 경우에는 메세지만 클라이언트로 전달한다.
				 */
				if(name.equals("")) {
					it_out.println(URLEncoder.encode(msg, "UTF-8"));
				}
				else if(user.equals(name)) {
					it_out.print("");
				}
				else {
					it_out.println("[" + 
							URLEncoder.encode(name, "UTF-8") 
							+ "]" + 
							URLEncoder.encode(msg, "UTF-8"));
				}
			} 
			catch (Exception e) {
				System.out.println("예외 : " +  e);
			}
		}	
	}	
	
	public void sendToMsg(String from_name, String to_name, String to_content) {
		
		Iterator<String> it = clientMap.keySet().iterator();
		
		while (it.hasNext()) {
			String user = it.next();
			try {
				PrintWriter it_out = (PrintWriter)
						clientMap.get(user);
				

					if(to_name.equals(user)) {
						it_out.println(from_name + "님이 보낸 귓속말" 
												+ to_content);	
					}
			} 
			catch (Exception e) {
				System.out.println("예외 : " +  e);
			}
		}	
	}	
	
	//내부클래스
	class MultiServerT extends Thread {
		
		//멤버변수
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		//생성자 : Socket을 기반으로 입출력 스트림을 생성한다.
		public MultiServerT(Socket socket) {
			this.socket = socket;
			
			try {
				
				out = new PrintWriter
						(this.socket.getOutputStream(), true);
				in = new BufferedReader(new 
				InputStreamReader(this.socket.getInputStream(), 
						"UTF-8"));
			} 
			catch (Exception e) {
				System.out.println("예외 : " + e);
			}
		}
		
		@Override
		public void run() {
			
			//클라이언트로부터 전송된 "대화명"을 저장 할 변수
			String name = "";
			//메세지 저장용 변수
			String s = "";
			
			try {
				//클라이언트의 이름을 읽어와서 저장
				name = in.readLine();
				name = URLDecoder.decode(name, "UTF-8");
				
				//접속한 클라이언트에게 새로운 사용자의 입장을 알림.
				//접속자를 제외한 나머지 클라이언트만 입장메세지를 받는다.
				sendAllMsg("", name + "님이 입장하셨습니다.");
				
				//현재 접속한 클라이언트를 HashMap에 저장한다.
				clientMap.put(name, out);
				
				//HashMap에 저장된 객체의 수로 접속자 수를 파악할 수 있다.
				System.out.println(name + "접속");
				System.out.println("현재 접속자 수는 " + 
								clientMap.size() + "명 입니다.");
				
				while (in != null) {
					
					s = in.readLine();
					s = URLDecoder.decode(s, "UTF-8");
					
					if(s == null)
						break;
					
					Iterator<String> mapIter = 
							clientMap.keySet().iterator();
					
					if(s.startsWith("/")) {
						
						//접속자 리스트
						if(s.substring(1).equals("list")) {
							out.println("\n현재 접속자 리스트");
							while(mapIter.hasNext()){
								String key = mapIter.next();
								out.println("[" + key + "] ");
							}
						}
						
						//귓속말 -> /to 홍길동 메세지
						if(s.substring(1,3).equals("to")) {

							String[] sArr = s.split(" ");
							
							//귓속말 상대를 입력하지 않았을 때
							if(sArr.length == 1) {
								out.println("귓속말 상대를 입력하세요.");
								continue;
							}
														
							String to_name = sArr[1];
							String to_content = "";
							
							//귓속말 고정
							if(sArr.length == 2) {
								out.println("");
								out.println(to_name + "에게 귓속말 고정됨");
								out.println("귓속말 고정 해제: x");
								while (true) {
									to_content = in.readLine();

									if(to_content.equalsIgnoreCase("x")) {
										out.println("귓속말 고정이 해제됨");
										break;
									}
									sendToMsg(name, to_name, to_content);
								}
							}	

							//1회용 귓속말
							else if(sArr.length >= 3) {
								for (int i = 2; i < sArr.length; i++) {
									to_content += " " + sArr[i];
								}
								sendToMsg(name, to_name, to_content);			
							}
						}
						
						continue;
					}
					
					String query = "INSERT INTO chating_tb VALUES "
							+ "(seq_chating.nextval, ?, ?, sysdate)";
					psmt = con.prepareStatement(query);
					
					psmt.setString(1, name);
					psmt.setString(2, s);
					psmt.executeUpdate();
					
					System.out.println(name + " >> " + s);
					sendAllMsg(name, s);
				}
			} 
			catch (Exception e) {
				System.out.println("예외 : " + e);
			}
			finally {
				/*
				클라이언트가 접속을 종료하면 예외가 발생하게 되어 finally로
				넘어오게 된다. 이때 "대화명"을 통해 remove()시켜준다.
				 */
				
				clientMap.remove(name);
				sendAllMsg("", name + "님이 퇴장하셨습니다.");
				//퇴장하는 클라이언트의 쓰레드명을 보여준다.
				System.out.println(name + " [" + 
				Thread.currentThread().getName() + "] 퇴장");
				System.out.println("현재 접속자 수는 " +
								clientMap.size() + "명 입니다.");
				
				try {
					in.close();
					out.close();
					socket.close();
					if(psmt != null) psmt.close();
					if(con != null) con.close();
					if(rs != null) rs.close();
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}

























