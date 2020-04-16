package chat7;

/*
서버에서...
1.클라이언트에서 대화가 올라옴
s = in.readLine();
현재 “이름 + 입력한 내용” 올라옴 → 입력한 내용만 올라오게 수업에서 만든 클라이언트 파일 수정해야 함

2.올라온 내용이 “/”로 시작하는지 검사
“/”로 시작하면 명령어임
아니면 일반 대화임
/list의 경우 명령어만 올라오므로 분리 없이 통째로 비교 가능함

3.명령어면 substring이나 StringTokenizer 로 내용 분리
분리한 내용에 list 가 있으면 해쉬맵의 키값만 묶어서 해당 사용자에게 내려 보냄
현재 해쉬맵에 키값이 사용자 이름임

4.귓속말로 분리한 내용의 첫 번째 요소에  to 가 있으면 귓속말 명령임
서버로 올라온 내용이 “/to 이름 내용” 이므로 이름을 분리해 냄
이름이 해쉬맵에서 키값으로 사용되고 있으므로 해당 키값의 밸류를 찾아 냄
현재 밸류값에 사용자와 연결된 Socket의 PrinterWriter 값이 저장되어 있슴
밸류값이 OutputStream 이므로 그걸 이용해서 해당 사용자에게만 내용을 내려보내면 귓속말이 구현됨

 */

import java.net.Socket;
import java.util.Scanner;


public class MultiClient {

	public static void main(String[] args) {
		
		
		
		//Sender가 기능을 가져가므로 여기서는 필요없음
//		PrintWriter out = null;
		//Receiver가 기능을 가져가므로 여기서는 필요없음
//		BufferedReader in = null;
		
		try {
			String ServerIP = "localhost";
			if(args.length > 0) {
				ServerIP = args[0];
			}
			Socket socket = new Socket(ServerIP, 9999);
			System.out.println("서버와 연결되었습니다..");
			
			
			String s_name = "";
			
			while (s_name.isEmpty()) {
				System.out.println("이름을 입력하세요.");
				Scanner scanner = new Scanner(System.in);
				s_name = scanner.nextLine();
			}
			
			
			//서버에서 보내는 Echo메세지를 클라이언트에 출력하기 위한 쓰레드 생성
			Thread receiver = new Receiver(socket);
			receiver.start();
			
			//클라이언트의 메세지를 서버로 전송해주는 쓰레드 생성
			Thread sender = new Sender(socket, s_name);
			sender.start();
			
		} 
		catch (Exception e) {
			System.out.println("예외발생[MultiClient]" + e);
		}
		
	}

}
