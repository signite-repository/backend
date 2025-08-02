# 사용자 프로필 API 가이드

## 개요

사용자 프로필 관리를 위한 종합적인 REST API 구현입니다. 프로필 조회/수정, 비밀번호 변경, 이메일 변경, 프로필 이미지 업로드, 계정 삭제 등의 기능을 제공합니다.

## 구현된 엔드포인트

### 1. 프로필 조회
```http
GET /api/users/profile
Authorization: Bearer {token}
```

**응답 예시:**
```json
{
  "id": "123",
  "username": "testuser",
  "email": "test@signight.com",
  "fullName": "홍길동",
  "bio": "안녕하세요, 개발자입니다.",
  "avatarUrl": "/uploads/avatars/123_1234567890.jpg",
  "phoneNumber": "010-1234-5678",
  "location": "서울, 대한민국",
  "website": "https://github.com/testuser",
  "createdAt": "2025-08-01T00:00:00Z",
  "updatedAt": "2025-08-02T00:00:00Z",
  "emailVerified": true,
  "twoFactorEnabled": false,
  "roles": ["USER"]
}
```

### 2. 프로필 수정
```http
PUT /api/users/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "fullName": "홍길동",
  "bio": "풀스택 개발자입니다.",
  "phoneNumber": "010-1234-5678",
  "location": "서울, 대한민국",
  "website": "https://github.com/testuser"
}
```

### 3. 비밀번호 변경
```http
POST /api/users/change-password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword456"
}
```

### 4. 이메일 변경
```http
POST /api/users/change-email
Authorization: Bearer {token}
Content-Type: application/json

{
  "newEmail": "newemail@signight.com",
  "password": "currentPassword"
}
```

### 5. 프로필 이미지 업로드
```http
POST /api/users/profile/avatar
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [이미지 파일]
```

**지원 형식:** JPG, JPEG, PNG, GIF

### 6. 계정 삭제
```http
DELETE /api/users/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "password": "currentPassword",
  "reason": "더 이상 사용하지 않음",
  "hardDelete": false  // true: 완전 삭제, false: 비활성화
}
```

### 7. 계정 활성화/비활성화
```http
POST /api/users/profile/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "enabled": false  // true: 활성화, false: 비활성화
}
```

### 8. 2단계 인증 설정
```http
POST /api/users/profile/2fa
Authorization: Bearer {token}
Content-Type: application/json

{
  "enable": true,
  "password": "currentPassword",
  "code": "123456"  // 확인 코드 (활성화 시)
}
```

## 프로필 기능 특징

### 1. 포괄적인 사용자 정보
- **기본 정보**: 이름, 소개, 위치, 웹사이트
- **연락처**: 이메일, 전화번호
- **보안**: 2단계 인증, 비밀번호 정책
- **개인정보 설정**: 프로필 공개 범위, 정보 노출 설정

### 2. 보안 기능
- **비밀번호 암호화**: BCrypt 사용
- **2단계 인증**: TOTP (Google Authenticator 호환)
- **계정 잠금**: 로그인 실패 시 일시 잠금
- **세션 관리**: 디바이스별 세션 추적

### 3. 프라이버시 설정
```kotlin
enum class ProfileVisibility {
    PUBLIC,      // 모든 사용자에게 공개
    FRIENDS,     // 친구에게만 공개  
    PRIVATE      // 비공개
}
```

### 4. 소셜 로그인 통합
- Google, Facebook, GitHub 계정 연동 지원
- 여러 소셜 계정을 하나의 프로필에 연결

## 구현 아키텍처

### 1. 도메인 모델
```kotlin
@Document("users")
data class User(
    val username: String,
    val email: String,
    val password: String,
    var fullName: String?,
    var bio: String?,
    var avatarUrl: String?,
    // ... 기타 필드
)
```

### 2. 서비스 계층
```kotlin
interface UserService {
    fun findByUsername(username: String): Mono<User>
    fun updateUser(user: User): Mono<User>
    fun deleteUser(userId: String): Mono<Void>
    fun existsByEmail(email: String): Mono<Boolean>
}
```

### 3. 파일 저장
프로필 이미지는 다음 옵션 중 선택:
- **로컬 스토리지**: 개발 환경
- **Amazon S3**: 프로덕션 권장
- **MinIO**: 온프레미스 S3 호환
- **MongoDB GridFS**: 작은 파일용

## 보안 고려사항

### 1. 인증 및 권한
- 모든 프로필 API는 JWT 토큰 필요
- 본인 프로필만 수정 가능
- 관리자는 모든 프로필 조회 가능

### 2. 입력 검증
- 이메일 형식 검증
- 비밀번호 복잡도 검증 (최소 8자, 대소문자, 숫자, 특수문자)
- 파일 크기 및 형식 제한

### 3. Rate Limiting
- 비밀번호 변경: 시간당 3회
- 이메일 변경: 일일 2회
- 프로필 이미지 업로드: 시간당 10회

## 사용 예시

### JavaScript/TypeScript
```typescript
// 프로필 조회
const response = await fetch('/api/users/profile', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const profile = await response.json();

// 프로필 수정
const updateResponse = await fetch('/api/users/profile', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    fullName: '홍길동',
    bio: '새로운 자기소개'
  })
});

// 프로필 이미지 업로드
const formData = new FormData();
formData.append('file', imageFile);

const uploadResponse = await fetch('/api/users/profile/avatar', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: formData
});
```

### React 컴포넌트 예시
```tsx
const ProfileEdit = () => {
  const [profile, setProfile] = useState({});
  
  const handleSubmit = async (data) => {
    const response = await updateProfile(data);
    if (response.ok) {
      toast.success('프로필이 업데이트되었습니다.');
    }
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <input name="fullName" placeholder="이름" />
      <textarea name="bio" placeholder="자기소개" />
      <button type="submit">저장</button>
    </form>
  );
};
```

## 향후 개선사항

1. **이메일 인증**: 이메일 변경 시 인증 메일 발송
2. **프로필 이미지 리사이징**: 썸네일 자동 생성
3. **활동 로그**: 프로필 변경 이력 추적
4. **프로필 백업**: 삭제 전 데이터 백업
5. **GDPR 준수**: 개인정보 다운로드 기능

프로필 편집 기능은 사용자 경험의 핵심 부분으로, 보안과 사용성을 모두 고려하여 구현되었습니다.