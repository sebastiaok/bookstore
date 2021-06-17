# Online Bookstore

본 과제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성하였습니다.  
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 개인과제 수행 결과입니다.


- 체크포인트 : https://workflowy.com/s/assessment/qJn45fBdVZn4atl3



# Table of contents

- [예제 - 온라인 서점](#---)

  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계) 
  - [구현:](#구현-)
    - [DDD 의 적용](#DDD-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)    
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일아웃 (HPA)](#오토스케일아웃_(HPA))
    - [ConfigMap](#ConfigMap)
    - [Zero-downtime deploy (Readiness Probe)](#Zero-downtime_deploy_(Readiness_Probe))
    - [Self-healing (Liveness Probe)](#Self-healing_(Liveness_Probe))



# 서비스 시나리오

기능적 요구사항

1. 고객이 책을 선택하여 주문을 한다
2. 고객이 결제한다
3. 결제가 완료되면 주문내역이 서점으로 전달된다
4. 서점에서 주문내역을 확인하여 배송을 한다
5. 고객이 주문을 취소할 수 있다
6. 주문이 취소되면 배송이 취소된다
7. 고객이 주문상태를 중간중간 조회한다
8. 주문상태가 바뀔 때 마다 카톡으로 알림을 보낸다

비기능적 요구사항

1. 트랜잭션
    1. 결제가 되지 않은 주문건은 아예 거래가 성립되지 않아야 한다 `Sync 호출` 
1. 장애격리
    1. 서점 배송 기능이 수행되지 않더라도 주문은 365일 24시간 받을 수 있어야 한다 `Async (event-driven)`, `Eventual Consistency`
    1. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다 `Circuit breaker`, `fallback`
1. 성능
    1. 고객은 주문상태를 언제든지 확인할 수 있어야 한다 `CQRS`
    1. 주문상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다 `Event driven`
    
    
    
# 체크포인트

- 분석 설계


  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?    
    

# 분석/설계


## AS-IS 조직 (Horizontally-Aligned)
![image](https://user-images.githubusercontent.com/81279673/122194131-1ec9e780-ced0-11eb-9e32-d3293f627e03.png)

## TO-BE 조직 (Vertically-Aligned)
![image](https://user-images.githubusercontent.com/81279673/122194242-399c5c00-ced0-11eb-8f8c-555fae742444.png)

## Event Storming 결과
* MSAEZ 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/pYauKq27pAMMO4ZZcMLRDtjzgIv1/share/40d9c225e0f9826deff3b8035d97b38f


### 이벤트 도출
![image](https://user-images.githubusercontent.com/81279673/122194313-49b43b80-ced0-11eb-8795-d3c4ebc8dfad.png)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/81279673/122194366-55076700-ced0-11eb-9e91-122cf58793ad.png)

    - 이벤트스토밍 과정 중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
    - UI이벤트는 대상에서 제외함

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/81279673/122194434-694b6400-ced0-11eb-946d-67f4d7576f56.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/81279673/122194484-736d6280-ced0-11eb-8f17-f089840f7751.png)

    - 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌

### 바운디드 컨텍스트로 묶기
![image](https://user-images.githubusercontent.com/81279673/122194557-84b66f00-ced0-11eb-9452-d2cb764b6345.png)

    - 도메인 서열 분리 
        - Core Domain:  app(front), store : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 app 의 경우 1주일 1회 미만, store 의 경우 1개월 1회 미만
        - Supporting Domain:   marketing, customer : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
        - General Domain:   pay : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 (핑크색으로 이후 전환할 예정)

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)
![image](https://user-images.githubusercontent.com/81279673/122195197-19b96800-ced1-11eb-9b71-9fcbfe2587d4.png)

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)
![image](https://user-images.githubusercontent.com/81279673/122195310-35bd0980-ced1-11eb-89e9-c003836bda80.png)

### 완성된 1차 모형
![image](https://user-images.githubusercontent.com/81279673/122315667-76f6fd00-cf55-11eb-8d7c-aef0294c98af.png)

    - View Model 추가
    - 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어)인 영어로 변경

### 기능적 요구사항 검증
![image](https://user-images.githubusercontent.com/81279673/122316109-3cda2b00-cf56-11eb-85a2-f4dfd24973d0.png)

    - 고객이 책을 선택하여 주문을 한다 (ok)
    - 고객이 결제한다 (ok)
    - 결제가 완료되면 주문내역이 서점으로 전달된다 (ok)    
    - 서점에서 주문내역을 확인하여 배송을 한다 (ok)    

![image](https://user-images.githubusercontent.com/81279673/122317341-541a1800-cf58-11eb-8ff9-df3b85667a1d.png)

    - 고객이 주문을 취소할 수 있다 (ok)
    - 주문이 취소되면 배송이 취소된다 (ok)
    - 고객이 주문상태를 중간중간 조회한다 (ok)
    - 주문상태가 바뀔 때 마다 카톡으로 알림을 보낸다 (ok)


### 비기능 요구사항 검증
![image](https://user-images.githubusercontent.com/81279673/122317171-0b625f00-cf58-11eb-9a86-c194b8394bce.png)

    - 트랜잭션
        1. 결제가 되지 않은 주문건은 아예 거래가 성립되지 않아야 한다 `Sync 호출` 
    - 장애격리
        2. 서점 배송 기능이 수행되지 않더라도 주문은 365일 24시간 받을 수 있어야 한다 `Async (event-driven)`, `Eventual Consistency`
        3. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다 `Circuit breaker`, `fallback`
    - 성능
        4. 고객은 주문상태를 언제든지 확인할 수 있어야 한다 `CQRS`
        5. 주문상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다 `Event driven`


### 완성된 모델
![image](https://user-images.githubusercontent.com/81279673/120965022-3520c680-c79f-11eb-9f13-dc80b8872f3f.png)

    - 수정된 모델은 모든 요구사항을 커버함.

## 헥사고날 아키텍처 다이어그램 도출
- 외부에서 들어오는 요청을 인바운드 포트를 호출해서 처리하는 인바운드 어댑터와 비즈니스 로직에서 들어온 요청을 회부 서비스를 호출해서 처리하는 아웃바운드 어댑터로 분리
- 호출관계에서 Pub/Sub 과 Req/Resp 를 구분함
- 서브 도메인과 바운디드 컨텍스트의 분리: 각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐
- 회의(Conference)의 경우 Polyglot 적용을 위해 Hsql로 설계

<img width="1200" alt="헥사고날 최종" src="https://user-images.githubusercontent.com/80210609/120962597-00ab0b80-c79b-11eb-9917-7c271b2a2434.PNG">


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 Bounded Context별로 마이크로서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다. (각 서비스의 포트넘버는 8081 ~ 8084, 8088 이다)

```
cd conference
mvn spring-boot:run

cd pay
mvn spring-boot:run 

cd room
mvn spring-boot:run  

cd customerCenter
mvn spring-boot:run 

cd gateway
mvn spring-boot:run
```

## DDD 의 적용

- msaez.io에서 이벤트스토밍을 통해 DDD를 작성하고 Aggregate 단위로 Entity를 선언하여 구현을 진행하였다.

> Conference 서비스의 Conference.java
```java
package hifive;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.persistence.*;

import java.util.Map;

@Entity
@Table(name="Conference_table")
public class Conference{

  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  private Long conferenceId;
  private String status;
  private Long payId;
  private Long roomNumber;

  @PrePersist //해당 엔티티를 저장한 후
  public void onPrePersist(){
      
    setStatus("CREATED");
    Applied applied = new Applied();
    //BeanUtils.copyProperties는 원본객체의 필드 값을 타겟 객체의 필드값으로 복사하는 유틸인데, 필드이름과 타입이 동일해야함.
    applied.setConferenceId(this.getConferenceId());
    applied.setConferenceStatus(this.getStatus());
    applied.setRoomNumber(this.getRoomNumber());
    applied.publishAfterCommit();
    //신청내역이 카프카에 올라감
    
    Map<String, String> res = ConferenceApplication.applicationContext
            .getBean(hifive.external.PayService.class)
            .paid(applied);
    //결제 아이디가 있고, 결제 상태로 돌아온 경우 회의 상태로 결제로 바꾼다.
    if (res.get("status").equals("Req_complete")) {
      this.setStatus("Req complete");
    }
    this.setPayId(Long.valueOf(res.get("payid")));

    return;
  }

  @PreRemove //해당 엔티티를 삭제하기 전 (회의를 삭제하면 취소신청 이벤트 생성)
  public void onPreRemove(){
    System.out.println("#################################### PreRemove : ConferenceId=" + this.getConferenceId());
    ApplyCanceled applyCanceled = new ApplyCanceled();
    applyCanceled.setConferenceId(this.getConferenceId());
    applyCanceled.setConferenceStatus("CANCELED");
    applyCanceled.setPayId(this.getPayId());
    applyCanceled.publishAfterCommit();
    //삭제하고 ApplyCanceled 이벤트 카프카에 전송
  }

  public Long getConferenceId() {
    return conferenceId;
  }
  public void setConferenceId(Long conferenceId) {
    this.conferenceId = conferenceId;
  }

  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

  public Long getPayId() {
    return payId;
  }
  public void setPayId(Long payId) {
    this.payId = payId;
  }

  public Long getRoomNumber() {
    return roomNumber;
  }
  public void setRoomNumber(Long roomNumber) {
    this.roomNumber = roomNumber;
  }
}

```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 기반의 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리 없이 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다.

> Conference 서비스의 ConferenceRepository.java
```java
package hifive;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="conferences", path="conferences")
public interface ConferenceRepository extends PagingAndSortingRepository<Conference, Long>{

}
```
> Conference 서비스의 PolicyHandler.java
```java
package hifive;

import java.util.Optional;
import hifive.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired ConferenceRepository conferenceRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverAssigned_UpdateStatus(@Payload Assigned assigned){
    
        if(!assigned.validate()) return;
        
        Optional<Conference> confOptional = conferenceRepository.findById(assigned.getConferenceId());
        Conference conference = confOptional.get();
        conference.setPayId(assigned.getPayId());
        conference.setStatus(assigned.getRoomStatus());
        conferenceRepository.save(conference);
    }
}
```

- 적용 후 REST API 의 테스트
```
# conference 서비스의 회의실 신청
http post http://localhost:8081/conferences status="" payId=0 roomNumber=1

# conference 서비스의 회의실 신청 취소
http delete http://localhost:8081/conferences/1

# 회의실 상태 확인
http GET http://localhost:8084/roomStates
```
> 회의실 신청 후 Conference 동작 결과
![Cap 2021-06-07 21-39-53-966](https://user-images.githubusercontent.com/80938080/121018071-f60f6700-c7d8-11eb-889d-fb674d1e8189.png)

## CQRS

- Materialized View 구현을 통해 다른 마이크로서비스의 데이터 원본에 접근없이 내 서비스의 화면 구성과 잦은 조회가 가능하게 하였습니다. 본 과제에서 View 서비스는 CustomerCenter 서비스가 수행하며 회의실 상태를 보여준다.

> 회의실 신청 후 customerCenter 결과
![Cap 2021-06-07 22-08-17-580](https://user-images.githubusercontent.com/80938080/121022024-edb92b00-c7dc-11eb-872b-23b51f1b1d57.png)

## 폴리글랏 퍼시스턴스

- 회의(conference)의 경우 H2 DB인 결제(pay)/회의실(room) 서비스와 달리 Hsql로 구현하여 MSA의 서비스간 서로 다른 종류의 DB에도 문제없이 동작하여 다형성을 만족하는지 확인하였다.

> pay, room 서비스의 pom.xml 설정
```xml
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
```
> conference 서비스의 pom.xml 설정
```xml
    <dependency>
        <groupId>org.hsqldb</groupId>
        <artifactId>hsqldb</artifactId>
        <scope>runtime</scope>
    </dependency>
```
## Gateway 적용
- API Gateway를 통하여 마이크로서비스들의 진입점을 단일화하였습니다.
> gateway > application.xml 설정
```yaml
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: conference
          uri: http://conference:8080
          predicates:
            - Path=/conferences/** 
        - id: pay
          uri: http://pay:8080
          predicates:
            - Path=/pays/** 
        - id: room
          uri: http://room:8080
          predicates:
            - Path=/rooms/** 
        - id: customerCenter
          uri: http://customerCenter:8080
          predicates:
            - Path= /roomStates/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 Conference -> Pay 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

> Pay 서비스의 external\PayService.java

```java
package hifive.external;

@FeignClient(name="pay", url="http://pay:8080")
public interface PayService {

    @RequestMapping(method= RequestMethod.GET, path="/pays/paid")
    public Map<String,String> paid(@RequestParam("status") String status, @RequestParam("conferenceId") Long conferenceId, @RequestParam("roomNumber") Long roomNumber);
}
```

- 예약을 받은 직후(@PostPersist) 결제를 요청하도록 처리

> Conference 서비스의 Conference.java (Entity)

```java
    @PostPersist //해당 엔티티를 저장한 후
    public void onPostPersist(){
    
        setStatus("CREATED");
        Applied applied = new Applied();
        //BeanUtils.copyProperties는 원본객체의 필드 값을 타겟 객체의 필드값으로 복사하는 유틸인데, 필드이름과 타입이 동일해야함.
        applied.setConferenceId(this.getConferenceId());
        applied.setConferenceStatus(this.getStatus());
        applied.setRoomNumber(this.getRoomNumber());
        applied.publishAfterCommit();
        //신청내역이 카프카에 올라감
        try {
            // 결제 서비스 Request
            Map<String, String> res = ConferenceApplication.applicationContext
                    .getBean(hifive.external.PayService.class)
                    .paid(applied);
            //결제 아이디가 있고, 결제 상태로 돌아온 경우 회의 상태로 결제로 바꾼다.
            if (res.get("status").equals("Req_complete")) {
                this.setStatus("Req complete");
            }
            this.setPayId(Long.valueOf(res.get("payid")));
            ConferenceApplication.applicationContext.getBean(javax.persistence.EntityManager.class).flush();
            return;
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 예약도 못받는다는 것을 확인:


```
# 결제 (pay) 서비스를 잠시 내려놓음 (ctrl+c)

# 결제 처리
http post http://localhost:8081/conferences status="" payId=0 roomNumber=1   #Fail
http post http://localhost:8081/conferences status="" payId=0 roomNumber=2   #Fail
```
> 결제 요청 오류 발생
![Cap 2021-06-07 22-24-26-184](https://user-images.githubusercontent.com/80938080/121024411-28bc5e00-c7df-11eb-9a84-d3095683d49c.png)
```
#결제서비스 재기동
cd pay
mvn spring-boot:run

#주문처리
http post http://localhost:8081/conferences status="" payId=0 roomNumber=1   #Success
http post http://localhost:8081/conferences status="" payId=0 roomNumber=2   #Success
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)


## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


결제가 이루어진 후에 회의실 관리(Room)로 이를 알려주는 행위는 동기식이 아니라 비동기식으로 처리하여 회의실 관리 서비스의 처리를 위하여 결제가 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 결제이력에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```java
package hifive;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Pay_table")
public class Pay {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long payId;
    private String status;
    private Long conferenceId;
    private Long roomNumber;

    @PostPersist
    public void onPostPersist(){

        if (this.getStatus() != "PAID") return;

        System.out.println("********************* Pay PostPersist Start. PayStatus=" + this.getStatus());

        Paid paid = new Paid();
        paid.setPayId(this.payId);
        paid.setPayStatus(this.status);
        paid.setConferenceId(this.conferenceId);
        paid.setRoomNumber(this.roomNumber);
        //BeanUtils.copyProperties(this, paid);
        paid.publishAfterCommit();

        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(toString());
        System.out.println("********************* Pay PostPersist End.");
    }

}
```
- 상점 서비스에서는 결제승인 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```java
package hifive;

@Service
public class PolicyHandler {
    @Autowired
    RoomRepository roomRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaid_RoomAssign(@Payload Paid paid) {

        if (!paid.validate()) {
            System.out.println("##### listener RoomAssign Fail");
            return;
        } else {
            System.out.println("\n\n##### listener RoomAssign : " + paid.toJson() + "\n\n");

            //예약 신청한 방 번호 조회, 퇴실 개념이 없기 때문에 상태 검사 하지 않음
            Optional<Room> optionalRoom = roomRepository.findById(paid.getRoomNumber());

            Room room = optionalRoom.get();
            room.setRoomStatus("FULL");
            room.setUsedCount(room.getUsedCount() + 1);
            room.setConferenceId(paid.getConferenceId());
            room.setPayId(paid.getPayId());

            System.out.println("##### 방배정 확인");
            System.out.println("[ RoomStatus : " + room.getRoomStatus() + ", RoomNumber : " + room.getRoomNumber() + ", UsedCount : " + room.getUsedCount() + ", ConferenceId : " + room.getConferenceId() + "," + room.getPayId() + "]");
            roomRepository.save(room);
        }
    }
}

```


회의실 관리 시스템은 주문/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 회의실 관리 시스템이 유지보수로 인해 잠시 내려간 상태라도 신청을 받는데 문제가 없다:
```
# 회의실 관리 시스템 (Room) 를 잠시 내려놓음 (ctrl+c)

#신청 처리
http post http://localhost:8081/conferences status="" payId=0 roomNumber=1   #Success
http post http://localhost:8081/conferences status="" payId=0 roomNumber=2   #Success

#신청 상태 확인
http localhost:8080/conferences     # 신청 상태 안바뀜 확인

#회의실 관리 서비스 기동
cd room
mvn spring-boot:run

#신청 상태 확인
http localhost:8080/conferences     # 모든 신청의 상태가 "할당됨"으로 확인
```


# 운영

## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 도커라이징, deploy 및
서비스 생성을 진행하였다.

- git에서 소스 가져오기
```
git clone https://github.com/jypark002/hifive.git
```
- Build 하기
```
cd hifive
cd conference
mvn package
```
- 도커라이징 : Azure 레지스트리에 도커 이미지 푸시하기
```
az acr build --registry skccuser05 --image skccuser05.azurecr.io/conference:latest .
```
- 컨테이너라이징 : 디플로이 생성 확인
```
kubectl create deploy conference --image=skccuser05.azurecr.io/conference:latest
```
- 컨테이너라이징 : 서비스 생성
```
kubectl expose deploy conference --port=8080
```
> customerCenter, pay, room, gateway 서비스도 동일한 배포 작업 반복

## 동기식 호출 / 서킷 브레이킹 / 장애격리

- Spring FeignClient + Hystrix을 사용하여 서킷 브레이킹 구현
- Hystrix 설정 : 결제 요청 쓰레드의 처리 시간이 410ms가 넘어서기 시작한 후 어느정도 지속되면 서킷 브레이커가 닫히도록 설정
- 결제를 요청하는 Conference 서비스에서 Hystrix 설정

> Conference 서비스의 application.yml 파일
```yaml
feign:
  hystrix:
    enabled: true
hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

- 결제 서비스(pay)에서 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
> Pay 서비스의 Pay.java 파일
```java
    @PostPersist
    public void onPostPersist(){
        if (this.getStatus() != "PAID") return;

        Paid paid = new Paid();
        paid.setPayId(this.payId);
        paid.setPayStatus(this.status);
        paid.setConferenceId(this.conferenceId);
        paid.setRoomNumber(this.roomNumber);
        paid.publishAfterCommit();

        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

- 부하테스터 siege 툴을 통한 서킷브레이커 동작 확인:
    - 동시사용자 100명
    - 60초 동안 실시

```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://52.231.34.176:8080/conferences POST {"status":"", "payId":0, "roomNumber":1}'
```
- 부하가 발생하고 서킷브레이커가 발동하여 요청 실패하였고, 밀린 부하가 다시 처리되면서 회의실 신청(Apply)를 받기 시작
![Cap 2021-06-08 10-37-57-954](https://user-images.githubusercontent.com/80938080/121108974-a450f600-c845-11eb-94ed-621b894f0da1.png)


- 운영 중인 시스템은 죽지 않고 지속적으로 서킷브레이커에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 하지만, 47.10% 가 성공하였고, 53%가 실패했다는 것은 고객 사용성에 있어 좋지 않기 때문에 Retry 설정과 동적 Scale out (replica의 자동적 추가,HPA) 을 통하여 시스템을 확장 해주는 후속처리가 필요.
![Cap 2021-06-08 10-39-01-129](https://user-images.githubusercontent.com/80938080/121109032-bdf23d80-c845-11eb-906b-9416924c6c1c.png)


## 오토스케일아웃 (HPA)
앞서 서킷브레이커는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 

- conference의 deployment.yaml 파일 설정

<img width="400" alt="야믈" src="https://user-images.githubusercontent.com/80210609/121058380-3b449080-c7fb-11eb-92ab-20852519d9d9.PNG">

- 신청서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:

```
kubectl autoscale deploy confenrence --min=1 --max=10 --cpu-percent=15
```

- hpa 설정 확인

<img width="600" alt="스케일-hpa" src="https://user-images.githubusercontent.com/80210609/121057419-37fcd500-c7fa-11eb-81ff-8d5062a219b4.PNG">


- CB 에서 했던 방식대로 워크로드를 1분 동안 걸어준다.
```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://conference:8080/conferences POST {"roomNumber": "123"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy conference -w
```

- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:
<img width="700" alt="스케일최종" src="https://user-images.githubusercontent.com/80210609/121056827-937a9300-c7f9-11eb-9ebc-ca86c271d3c3.PNG">

- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 
<img width="600" alt="상태" src="https://user-images.githubusercontent.com/80210609/121057028-cde43000-c7f9-11eb-88d2-c022dddca49f.PNG">
  

## ConfigMap
- 환경정보로 변경 시 ConfigMap으로 설정함

- 리터럴 값으로부터 ConfigMap 생성
![image](https://user-images.githubusercontent.com/81279673/121073309-4ef8f280-c80d-11eb-998e-d13b361d53e4.png)

- 설정된 ConfigMap 정보 가져오기
![image](https://user-images.githubusercontent.com/81279673/121074021-42c16500-c80e-11eb-8db8-2497dcc099e1.png)
![image](https://user-images.githubusercontent.com/81279673/121073595-a9924e80-c80d-11eb-80e5-88b40effb31b.png)

- 관련된 프로그램(application.yaml, PayService.java) 적용
![image](https://user-images.githubusercontent.com/81279673/121073814-fe35c980-c80d-11eb-980b-5dcc1c6d7019.png)
![image](https://user-images.githubusercontent.com/81279673/121073824-ffff8d00-c80d-11eb-8bda-cc188492d138.png)

## Zero-downtime deploy (Readiness Probe)
- Room 서비스에 kubectl apply -f deployment_non_readiness.yml 을 통해 readiness Probe 옵션을 제거하고 컨테이너 상태 실시간 확인
![non_de](https://user-images.githubusercontent.com/47212652/121105020-32c17980-c83e-11eb-8e10-c27ee89a369d.PNG)

- Room 서비스에 kubectl apply -f deployment.yml 을 통해 readiness Probe 옵션 적용
- readinessProbe 옵션 추가  
    > initialDelaySeconds: 10  
    > timeoutSeconds: 2  
    > periodSeconds: 5  
    > failureThreshold: 10  

- 컨테이너 상태 실시간 확인
![dep](https://user-images.githubusercontent.com/47212652/121105025-33f2a680-c83e-11eb-9db0-ee2206a966fe.PNG)

## Self-healing (Liveness Probe)
- Pay 서비스에 kubectl apply -f deployment.yml 을 통해 liveness Probe 옵션 적용

- liveness probe 옵션을 추가
- initialDelaySeconds: 10
- timeoutSeconds: 2
- periodSeconds: 5
- failureThreshold: 5
                 
  ![스크린샷 2021-06-08 오후 2 16 45](https://user-images.githubusercontent.com/40500484/121127061-2cde8f00-c864-11eb-8b4f-7d3abcba60b3.png)


- Pay 서비스에 liveness가 적용된 것을 확인

- Http Get Pay/live를 통해서 컨테이너 상태 실시간 확인 및 재시동 

  
  ![스크린샷 2021-06-07 오후 9 45 31](https://user-images.githubusercontent.com/40500484/121018788-c9a81a80-c7d9-11eb-9013-1a68ccf1a9b1.png)


- Liveness test를 위해 port : 8090으로 변경
- Delay time 등 옵션도 작게 변경
  
  ![스크린샷 2021-06-08 오후 1 59 29](https://user-images.githubusercontent.com/40500484/121125804-1cc5b000-c862-11eb-8d5d-34b5a0ba1df2.png)

- Liveness 적용된 Pay 서비스 , 응답불가로 인한 restart 동작 확인

  ![스크린샷 2021-06-08 오후 1 59 15](https://user-images.githubusercontent.com/40500484/121125928-50083f00-c862-11eb-91dd-c47a74eade37.png)
