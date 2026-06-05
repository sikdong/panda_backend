# ECS Fargate 학습 로드맵

이 문서는 `panda_backend` 프로젝트를 기준으로 Docker, ECR, ECS Fargate, 무중단 배포, Auto Scaling, 운영 관측을 순서대로 학습하기 위한 실행 가이드다.

## 현재 로컬 기준선

- App container port: `9111`
- Health check path: `/health`
- Version path: `/version`
- Local app URL: `http://localhost:9111`
- Local MySQL host port: `3307`
- Compose service names: `app`, `mysql`

로컬 확인:

```powershell
docker compose up --build -d
curl http://localhost:9111/health
curl http://localhost:9111/version
```

기대 결과:

```text
OK
v1
```

## 2단계: ECR 이미지 저장소

목표는 로컬에서 빌드한 Docker 이미지를 AWS ECR에 올리는 것이다.

필요한 값:

- AWS region: 예시 `ap-northeast-2`
- ECR repository name: `panda-backend`
- Image tag: `v1`, `v2`처럼 명시 버전 사용

흐름:

```powershell
aws ecr create-repository --repository-name panda-backend --region ap-northeast-2

aws ecr get-login-password --region ap-northeast-2 `
  | docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com

docker build -t panda-backend:v1 .
docker tag panda-backend:v1 <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/panda-backend:v1
docker push <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/panda-backend:v1
```

학습 포인트:

- ECR은 컨테이너 이미지 저장소다.
- ECS Task Definition은 ECR image URI를 참조한다.
- `latest`만 쓰면 배포 이력과 롤백이 불명확해진다.

## 3단계: ECS Fargate 첫 배포

목표는 서버를 직접 관리하지 않고 Fargate task로 Spring Boot 컨테이너를 실행하는 것이다.

필수 구성:

- VPC
- Public subnet 2개 이상
- ECS cluster
- Task definition
- ECS service
- Application Load Balancer
- Target group
- CloudWatch log group

Task definition 주요 값:

- Launch type: `FARGATE`
- CPU: `0.5 vCPU`부터 시작
- Memory: `1 GB`부터 시작
- Container image: `<account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/panda-backend:v1`
- Container port: `9111`
- Log driver: `awslogs`

ALB target group 주요 값:

- Protocol: `HTTP`
- Target type: `ip`
- Port: `9111`
- Health check path: `/health`
- Success code: `200`

배포 후 확인:

```powershell
curl http://<alb-dns-name>/health
curl http://<alb-dns-name>/version
```

## 4단계: Rolling 무중단 배포

목표는 기존 task를 유지한 상태에서 새 버전 task를 띄우고, health check 통과 후 트래픽을 넘기는 것이다.

ECS service 권장 시작값:

- Desired tasks: `2`
- Minimum healthy percent: `100`
- Maximum percent: `200`
- Deployment circuit breaker: enabled
- Rollback on failure: enabled

실습 흐름:

1. `/version`이 `v1`인 이미지를 배포한다.
2. `/version` 응답을 `v2`로 바꾼다.
3. `panda-backend:v2` 이미지를 빌드해서 ECR에 push한다.
4. Task definition revision을 새로 만든다.
5. ECS service를 새 task definition으로 update한다.
6. `curl /version`을 반복 호출해서 `v1`에서 `v2`로 전환되는지 본다.

확인 명령 예시:

```powershell
while ($true) {
  curl http://<alb-dns-name>/version
  Start-Sleep -Seconds 1
}
```

## 5단계: 실패 배포와 롤백

목표는 운영에서 실패한 배포가 어떻게 감지되고 되돌아가는지 보는 것이다.

실패 유도 방법:

- 잘못된 image tag를 task definition에 넣기
- 앱이 시작 직후 종료되도록 만들기
- `/health`가 `500`을 반환하도록 만들기

관찰할 위치:

- ECS service events
- ECS deployment status
- Target group health
- CloudWatch logs

## 6단계: ECS Service Auto Scaling

목표는 부하에 따라 task 수가 자동으로 늘고 줄어드는 것을 확인하는 것이다.

권장 시작값:

- Minimum tasks: `2`
- Desired tasks: `2`
- Maximum tasks: `6`
- Policy type: Target tracking
- Metric: `ECSServiceAverageCPUUtilization`
- Target value: `50` 또는 `60`

부하 테스트 예시:

```powershell
docker run --rm williamyeh/wrk -t4 -c100 -d60s http://<alb-dns-name>/health
```

관찰할 지표:

- ECS running task count
- CPU utilization
- ALB request count
- Target response time
- Scale-out/scale-in event

## 7단계: 컨테이너 운영 관측

목표는 장애가 났을 때 어디를 봐야 하는지 익히는 것이다.

반드시 볼 것:

- CloudWatch Logs
- ECS service events
- ECS task stopped reason
- ALB target group health
- ALB 4xx/5xx metrics
- CPU/memory metrics

초기 알람 후보:

- ALB `HTTPCode_Target_5XX_Count`
- Target group `UnHealthyHostCount`
- ECS service CPU high
- ECS service memory high

## 8단계: Blue/Green 배포

Rolling update에 익숙해진 뒤 진행한다.

목표는 blue 환경과 green 환경을 분리하고, 새 버전이 정상일 때 트래픽을 전환하는 것이다.

주요 구성:

- ECS service
- ALB listener
- Blue target group
- Green target group
- CodeDeploy 또는 ECS blue/green deployment
- Traffic shifting policy

학습 순서:

1. Rolling update의 한계를 이해한다.
2. Blue/Green에서 target group이 왜 2개 필요한지 확인한다.
3. All-at-once 전환부터 실습한다.
4. Canary 또는 linear traffic shifting으로 확장한다.
5. 실패 시 rollback 동작을 확인한다.

## 9단계: CI/CD 자동화

목표는 main branch에 변경이 들어가면 자동으로 이미지 빌드, ECR push, ECS 배포가 되게 하는 것이다.

GitHub Actions 흐름:

1. Checkout
2. Java setup
3. Gradle build 또는 bootJar
4. Docker build
5. ECR login
6. Docker push
7. ECS task definition render
8. ECS service deploy

운영 전 확인할 것:

- AWS credential은 GitHub secret 또는 OIDC로 관리한다.
- DB 비밀번호는 compose처럼 평문으로 두지 않는다.
- AWS에서는 Secrets Manager 또는 SSM Parameter Store를 사용한다.
- image tag는 commit SHA 또는 release version을 사용한다.
