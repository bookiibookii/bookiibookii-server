# 부키부키 서버팀 README
<img width="7680" height="4320" alt="배너2" src="https://github.com/user-attachments/assets/bd0d766f-fce3-45df-ab09-5defda749bb1" />

<br><br>

## 🔎 About the Project
![x배너 (1)](https://github.com/user-attachments/assets/40112447-e22d-4cb8-b611-8395f2274232)
> design by loverlikewater@gmail.com / @aoree.lim

<br>

## 🤝 Team
| 강예손 | 박성진 | 박현서 | 인석진 | 한태빈 |
|:------:|:------:|:------:|:------:|:------:|
| <img width="179" height="179" alt="강예손" src="https://github.com/user-attachments/assets/6340e5ca-d41a-4769-aed8-cf0a7fc8e252" /> | <img src="이미지주소" width="120"/> | <img src="이미지주소" width="120"/> | <img src="이미지주소" width="120"/> |
| [@kangyeson](https://github.com/kangyeson) | [@macboy5](https://github.com/macboy5) | [@gyeonseo](https://github.com/yc3697) | [@sjinssun](https://github.com/sjinssun) | [@taebin2](https://github.com/taebin2) |

<br/>

## 🛠️ Server Architecture
<img width="872" height="585" alt="서버아키텍처" src="https://github.com/user-attachments/assets/2f707c63-5eb5-41ad-9522-77cda4711288" />


<br><br>

## ☁️ ERD
<img width="3160" height="2432" alt="ERD" src="https://github.com/user-attachments/assets/87a2c9d5-f45c-45c6-ad67-393277843032" />

<br><br>

## 🛠 Tech Stack
**Language**: Java 17<br/>
**Framework**: Spring Boot 4.0.1, Spring MVC<br/>
**Data**: Spring Data JPA, QueryDSL (Type-safe Query)<br/>
**Security**: Spring Security, JWT (Role-based Access Control)<br/>

- **Infrastructure & Storage**
  - **Database**: MySQL (AWS RDS - Logical Schema Separation)<br/>
  - **Cache**: Redis (AWS ElastiCache - Namespace Isolation)<br/>
  - **Storage**: AWS S3 (Presigned URL Architecture)<br/>
  - **Computing**: AWS EC2 (Environment Separation: Dev / Prod)<br/>
  - **Network**: AWS ALB (Host-based Routing), Route 53, ACM (HTTPS)<br/>

- **DevOps**
  - **CI/CD**: GitHub Actions, SSH Deploy

<br>

## 🌿 Branch Strategy
- `main` : 배포용(CI/CD) 브랜치
- `develop` : 개발 통합 브랜치 (default)
- `feature` : 이슈 단위 기능 개발 브랜치

**{type}/#Issue Number-{작업 내용}** => 공백은 - 로 연결
> feat/#1-kakao-login<br>
> mod/#15-mypage-dto

<br>

## 📖 Pull Request Convention
**[type] 이슈 제목**
> [Feat] 로그인 기능 구현
- PR 생성 시 24시간 이내에 확인을 요합니다.
- develop 브랜치로의 병합은 최소 1명 이상의 리뷰어 승인(Approve) 이 필요합니다.

<br>

## 📖 Commit Convention
**[type] #Issue Number 제목(작업 내용)**
본문 (한줄로 설명 가능한 경우 본문은 생략) 
- `Feat` : 새로운 기능 구현
- `Mod` : 코드 및 내부 파일 수정
- `Add` : feat 이외의 부수적인 코드, 파일, 라이브러리 추가
- `Delete` : 불필요한 코드나 파일 삭제
- `Fix` : 버그 및 오류 해결
- `Chore` : 의존성 추가, 패키지 구조, 함수 및 변수명 변경 등의 작은 작업
- `HOTFIX` : 배포된 버전에 이슈 발생 시, 긴급하게 수정 작업
- `Rename` : 파일이나 폴더명 수정
- `Docs` : README나 Wiki 등의 문서 작업
- `Refactor` : 로직 변경 없이 기존의 코드를 개선하는 리팩터링
- `Comment` : 필요한 주석 추가 및 변경
> [Feat] #11 로그인 서버 연동
