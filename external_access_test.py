#!/usr/bin/env python3
"""
외부 접근성 테스트 스크립트
- HTTP API 접근 테스트
- WebSocket 연결 테스트  
- 네트워크 포트 확인
- 외부 IP를 통한 접근 시도
"""

import requests
import websocket
import socket
import subprocess
import time
import json
import threading
from urllib.parse import urlparse

class ExternalAccessTester:
    def __init__(self):
        self.spring_boot_url = "http://localhost:8080"
        self.websocket_url = "ws://localhost:8080"
        self.websocket_http_url = "http://localhost:3001"
        self.external_ip = None
        
    def get_external_ip(self):
        """외부 IP 주소 조회"""
        try:
            response = requests.get('https://api.ipify.org', timeout=5)
            self.external_ip = response.text.strip()
            print(f"✅ 외부 IP: {self.external_ip}")
            return self.external_ip
        except Exception as e:
            print(f"❌ 외부 IP 조회 실패: {e}")
            return None
    
    def check_port_open(self, host, port, timeout=5):
        """포트 열림 상태 확인"""
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(timeout)
            result = sock.connect_ex((host, port))
            sock.close()
            return result == 0
        except Exception as e:
            print(f"❌ 포트 체크 실패 ({host}:{port}): {e}")
            return False
    
    def test_spring_boot_api(self):
        """Spring Boot API 접근 테스트"""
        print("\n🌐 Spring Boot API 테스트")
        print("-" * 50)
        
        endpoints = [
            "/api/test/health",
            "/api/category", 
            "/.well-known/jwks.json",
            "/.well-known/openid_configuration"
        ]
        
        # 로컬 접근 테스트
        print("📍 로컬 접근 테스트:")
        for endpoint in endpoints:
            url = f"{self.spring_boot_url}{endpoint}"
            try:
                response = requests.get(url, timeout=5)
                status = "✅ 성공" if response.status_code < 400 else f"⚠️ {response.status_code}"
                print(f"  {endpoint}: {status}")
            except requests.exceptions.ConnectionError:
                print(f"  {endpoint}: ❌ 연결 거부 (서버 미실행)")
            except Exception as e:
                print(f"  {endpoint}: ❌ 오류 - {e}")
        
        # 포트 확인
        port_open = self.check_port_open('localhost', 8080)
        print(f"📡 포트 8080 상태: {'✅ 열림' if port_open else '❌ 닫힘'}")
        
        # 외부 IP로 접근 테스트 (포트포워딩이 설정된 경우)
        if self.external_ip:
            print(f"\n📍 외부 IP 접근 테스트 ({self.external_ip}):")
            external_port_open = self.check_port_open(self.external_ip, 8080, timeout=3)
            print(f"📡 외부 포트 8080 상태: {'✅ 접근 가능' if external_port_open else '❌ 접근 불가'}")
            
            if external_port_open:
                try:
                    external_url = f"http://{self.external_ip}:8080/api/test/health"
                    response = requests.get(external_url, timeout=10)
                    print(f"  외부 API 접근: ✅ 성공 ({response.status_code})")
                except Exception as e:
                    print(f"  외부 API 접근: ❌ 실패 - {e}")
    
    def test_websocket_server(self):
        """WebSocket 서버 접근 테스트"""
        print("\n⚡ WebSocket 서버 테스트")
        print("-" * 50)
        
        # HTTP API 테스트 (헬스체크)
        print("📍 WebSocket HTTP API:")
        try:
            response = requests.get(f"{self.websocket_http_url}/health", timeout=5)
            print(f"  헬스체크: ✅ 성공 ({response.status_code})")
            health_data = response.json()
            print(f"  서비스 상태: {health_data.get('status', 'unknown')}")
        except requests.exceptions.ConnectionError:
            print(f"  헬스체크: ❌ 연결 거부 (서버 미실행)")
        except Exception as e:
            print(f"  헬스체크: ❌ 오류 - {e}")
        
        # WebSocket 연결 테스트
        print("\n📍 WebSocket 연결 테스트:")
        ws_port_open = self.check_port_open('localhost', 8080)
        print(f"📡 WebSocket 포트 상태: {'✅ 열림' if ws_port_open else '❌ 닫힘'}")
        
        if ws_port_open:
            try:
                def on_open(ws):
                    print("  WebSocket 연결: ✅ 성공")
                    # 테스트 메시지 전송
                    test_message = {
                        "type": "Join",
                        "room_id": "test-room",
                        "name": "TestPlayer",
                        "color": "#FF0000"
                    }
                    ws.send(json.dumps(test_message))
                
                def on_message(ws, message):
                    print(f"  메시지 수신: ✅ {message[:100]}...")
                    ws.close()
                
                def on_error(ws, error):
                    print(f"  WebSocket 오류: ❌ {error}")
                
                def on_close(ws, close_status_code, close_msg):
                    print("  WebSocket 연결 종료")
                
                ws = websocket.WebSocketApp(self.websocket_url,
                                          on_open=on_open,
                                          on_message=on_message,
                                          on_error=on_error,
                                          on_close=on_close)
                
                # 타임아웃을 위한 별도 스레드
                def timeout_close():
                    time.sleep(5)
                    try:
                        ws.close()
                    except:
                        pass
                
                threading.Thread(target=timeout_close, daemon=True).start()
                ws.run_forever()
                
            except Exception as e:
                print(f"  WebSocket 연결: ❌ 실패 - {e}")
    
    def test_firewall_and_network(self):
        """방화벽 및 네트워크 설정 확인"""
        print("\n🔥 방화벽 및 네트워크 설정")
        print("-" * 50)
        
        # Windows 방화벽 상태 확인 (PowerShell 필요)
        try:
            result = subprocess.run([
                'powershell', '-Command', 
                'Get-NetFirewallProfile | Select-Object Name,Enabled'
            ], capture_output=True, text=True, timeout=10)
            
            if result.returncode == 0:
                print("🛡️ Windows 방화벽 상태:")
                print(result.stdout)
            else:
                print("🛡️ 방화벽 상태 확인 실패")
        except Exception as e:
            print(f"🛡️ 방화벽 확인 불가: {e}")
        
        # 네트워크 인터페이스 확인
        try:
            result = subprocess.run(['ipconfig'], capture_output=True, text=True, timeout=10)
            if result.returncode == 0:
                lines = result.stdout.split('\n')
                print("\n🌐 네트워크 인터페이스:")
                for line in lines:
                    if 'IPv4' in line or 'Default Gateway' in line:
                        print(f"  {line.strip()}")
        except Exception as e:
            print(f"🌐 네트워크 정보 확인 불가: {e}")
    
    def generate_external_access_guide(self):
        """외부 접근 설정 가이드 생성"""
        print("\n📋 외부 접근 설정 가이드")
        print("=" * 50)
        
        guide = """
🔧 외부 접근을 위한 설정 단계:

1. 🌐 포트포워딩 설정 (공유기/라우터)
   - 포트 8080 → 내부 IP:8080 (Spring Boot)
   - 포트 3001 → 내부 IP:3001 (WebSocket HTTP)
   
2. 🛡️ Windows 방화벽 설정
   - 인바운드 규칙: 포트 8080, 3001 허용
   - 명령어: netsh advfirewall firewall add rule name="Signight API" dir=in action=allow protocol=TCP localport=8080
   
3. 🚀 서버 실행 확인
   - Spring Boot: ./gradlew bootRun
   - WebSocket: cargo run
   
4. 🧪 외부 접근 테스트
   - http://[외부IP]:8080/api/test/health
   - ws://[외부IP]:8080 (WebSocket)
   
5. 🔒 보안 설정 (프로덕션)
   - HTTPS/WSS 사용
   - JWT 토큰 검증
   - Rate Limiting 설정
   - CORS 정책 적용
"""
        print(guide)
    
    def run_all_tests(self):
        """모든 테스트 실행"""
        print("🚀 Signight 외부 접근성 테스트")
        print("=" * 50)
        
        # 외부 IP 조회
        self.get_external_ip()
        
        # 각종 테스트 실행
        self.test_spring_boot_api()
        self.test_websocket_server()
        self.test_firewall_and_network()
        self.generate_external_access_guide()
        
        print("\n✅ 테스트 완료!")

if __name__ == "__main__":
    tester = ExternalAccessTester()
    tester.run_all_tests()