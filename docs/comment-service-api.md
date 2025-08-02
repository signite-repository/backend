# 💬 Comment Service API 매뉴얼

## 1. 개요

- **담당 서비스**: `comment-service`
- **기술 스택**: Rust, Actix-web, MongoDB
- **주요 기능**:
  - 게시글에 대한 댓글 CRUD
  - 대댓글(Nested Comments) 지원
  - 빠르고 가벼운 댓글 처리에 최적화

---

## 2. API 엔드포인트

- **Base URL**: `/api/comments`

### **특정 게시글의 댓글 목록 조회**
- `GET /?postId={postId}`
- **설명**: 특정 `postId`에 해당하는 모든 댓글을 계층 구조로 조회합니다.
- **인증**: 불필요
- **Response (200 OK)**:
  ```json
  [
    {
      "id": "comment_id_1",
      "postId": "post_id_1",
      "username": "user1",
      "content": "첫 번째 댓글입니다.",
      "createdAt": "...",
      "replies": [
        {
          "id": "comment_id_2",
          "postId": "post_id_1",
          "username": "user2",
          "content": "대댓글입니다.",
          "createdAt": "...",
          "replies": []
        }
      ]
    }
  ]
  ```

### **댓글 작성**
- `POST /`
- **설명**: 새로운 댓글 또는 대댓글을 작성합니다.
- **인증**: `ACTIVE_MEMBER` 이상 권한 필요
- **Request Body**:
  ```json
  {
    "postId": "post_id_1",
    "content": "새로운 댓글 내용",
    "parentId": "comment_id_1" // 대댓글일 경우 부모 댓글 ID
  }
  ```
- **Response (201 CREATED)**: 생성된 댓글 정보 반환

### **댓글 수정**
- `PUT /{id}`
- **설명**: `id`에 해당하는 댓글 내용을 수정합니다.
- **인증**: 작성자 본인 또는 `ADMIN` 권한
- **Request Body**:
  ```json
  {
    "content": "수정된 댓글 내용"
  }
  ```

### **댓글 삭제**
- `DELETE /{id}`
- **설명**: `id`에 해당하는 댓글을 삭제합니다. (실제 DB에서는 `is_deleted` 플래그 처리)
- **인증**: 작성자 본인 또는 `ADMIN` 권한

---

## 3. 데이터베이스 (MongoDB)

- **컬렉션 이름**: `comments`
- **문서 구조**:
  ```javascript
  {
    _id: ObjectId("..."),
    postId: "post_id_1",          // 게시글 ID
    userId: "user_id_1",          // 작성자 ID
    username: "testuser",         // 작성자 이름 (비정규화)
    content: "댓글 내용입니다.",   // 댓글 내용
    parentId: "parent_comment_id",// 부모 댓글 ID (대댓글용)
    createdAt: ISODate("..."),
    updatedAt: ISODate("..."),
    is_deleted: false             // 삭제 여부 플래그
  }
  ```
- **인덱스**:
  - `postId`와 `createdAt`에 대한 복합 인덱스를 사용하여 특정 게시글의 댓글을 시간순으로 빠르게 조회합니다.
  - `parentId` 인덱스를 사용하여 대댓글 조회를 최적화합니다.
