이 문서는 SSH 서버 데몬의 내부 동작 및 사용 방법을 설명한다.
 

데몬의 초기화 과정
------------------
SshDaemon은 서버 실행 후 Global 객체가 생성 될 때 Global 객체 내부의 맴버 인스턴스로 생성된다.
Global 객체의 onStart메서드가 실행 될 때, 이미 생성 되어 있는 SshDaemon 인스턴스의 start메서드를 호출하여 데몬을 초기화하고 실행한다.
ssh가 사용하는 포트는 기본적으로 22번을 사용하며 conf/application.conf 의 ssh.port에 값이 지정되어 있다면 해당 포트로 초기화 한다.
데몬이 시작되면 사용자는 ssh프로토콜을 통해 git 저장소에 접근이 가능해 진다.
만약 데몬 시작에 실패했다면 yobi의 프로젝트 홈 화면에서 ssh 주소 복사 기능이 노출되지 않는다.


SSH 서버의 SSH키 생성
---------------------
SshDaemon이 실행된 후 최초로 ssh사용자가 접근을 시도되면 PEMGeneratorHostKeyProvider를 통해 서버의 ssh키가 생성된다.
생성된 키 파일은 yobi서버의 홈 디렉터리에 yobihostkey.pem 파일로 저장되며 한번 생성 된 후에는 다시 생성하지 않고 동일한 키를 사용한다.
만약 서버의 키 파일이 변경됐다면 과거에 해당 서버에 접속했던 사용자는 다음과 같은 경고 메시지와 함께 접속을 거절당한다.
```
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@    WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!     @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!
Someone could be eavesdropping on you right now (man-in-the-middle attack)!
It is also possible that a host key has just been changed.
The fingerprint for the RSA key sent by the remote host is
[...]
Please contact your system administrator.
Add correct host key in ~/.ssh/known_hosts to get rid of this message.
Offending RSA key in ~/.ssh/known_hosts:1
RSA host key for localhost has changed and you have requested strict checking.
Host key verification failed.
```
이 경우 사용자는 ~/.ssh/known_hosts파일을 수정해야 접속이 가능하다.
- known_hosts파일은 사용자가 ssh로 접속을 시도했던 서버들의 공개키 정보를 담고 있다.

**known_hosts파일 수정 방법**
```
$ ssh-keygen -R "hostname or ip address"

위 명령은 known_hosts에서 hostname이나 ip address에 해당하는 공개키 정보를 삭제한다.
이 후 다시 ssh로 접속을 시도하면 다음과 같은 메시지와 함께 known_hosts에 새로운 키를 등록한다.

The authenticity of host 'localhost (127.0.0.1)' can't be established.
RSA key fingerprint is [...].
Are you sure you want to continue connecting (yes/no)? yes
Warning: Permanently added 'localhost' (RSA) to the list of known hosts.
```


SSH 주소 형식
-------------
```
ssh://yobi@domain:port/owner/project
```

ssh접근에 사용 하는 계정명(username)은 yobi로 통일되어 있다.
포트 번호가 22번(well-known)일 경우 :port는 생략이 가능하다.


사용자의 인증 과정
------------------
ssh사용자가 접근을 시도하면 데몬은 SshPublicKeyAuth를 통해 사용자가 제공한 공개키(public key)를 확인한다.
먼저, 사용자가 제공한 공개키와 일치하는 공개키가 데이터베이스에 존재하지 않을 경우는 다음과 같이 처리한다.
- 1. 서버는 클라이언트에게 실패했다는 정보`SshConstants.SSH_MSG_USERAUTH_FAILURE (51)`를 담은 메시지를 전달한다.
- 2. 클라이언트는 이 메시지를 바탕으로 사용자의 터미널에 `Permission denied`를 출력하고 연결을 종료한다.

다음으로, 공개키가 일치하는 경우에는 다음과 같이 처리한다.
- 1. 서버는 클라이언트에게 공개키가 유효하다는 정보`SshConstants.USERAUTH_PK_OK (60)`를 담은 메시지를 전달하고 응답을 기다린다.
- 2. 클라이언트는 공개키와 매치되는 개인키의 passphrase를 사용자에게 입력하도록 요구한다.
- 3-1. 만약, 사용자가 passphrase를 3회까지 틀릴 경우 더 이상 인증이 진행되지 않고 종료된다.
- 3-2. 반대로 passphrase를 정상적으로 입력했다면 클라이언트는 공개키-개인키쌍이 성립한다는 정보(true)와, 암호화 알고리즘 방식을 서버에 전달한다.
- 4. 서버는 이를 바탕으로 응답확인 정보`SshConstants.SSH_MSG_USERAUTH_REQUSET (50)`와 인증성공 정보`SshConstants.SSH_MSG_USERAUTH_SUCCESS (52)`를 포함하는 메시지를 클라이언트에게 전달한다.


SSH Daemon의 쉘 접근
-------------------- 
Yobi의 ssh데몬은 git서비스를 위한 데몬이므로 ssh 클라이언트를 사용한 직접 접근은 제한되어야 한다.
그러므로 사용자가 ssh 클라이언트를 통해 서버에 직접 접근 하는 경우,
데몬의 YobiSshShellFactory에서 직접 접근이 불가능함을 알리는 메세지를 클라이언트에 출력하고 사용자의 접속을 종료시킨다.
사용자의 git 클라이언트로부터 clone, push, pull 등 을 위한 git-upload-pack 혹은 git-receive-pack 명령이 전송되면
SshCommandFactory를 통해 명령어를 확인하고 서비스 가능한 명령일 시 실행한다. (아닐 경우 UnknownCommand로 예외 처리한다.)


사용자의 공개키(publickey) 등록
-----------------------
Yobi의 사용자는 설정 페이지의 ssh키 설정 메뉴를 통해 본인의 공개키를 등록 할 수 있다.
공개키는 사용자가 작성한 키 파일의 설명과 함께 등록이 가능하다.
등록된 키는 최초 등록일, 마지막 사용 날짜가 출력되며 만약 공개키가 도용되어 인증에 실패한 요청도 사용한 날짜로 간주하여 업데이트한다.
