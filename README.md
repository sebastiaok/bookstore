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
    - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
    - [이벤트드리븐 아키텍쳐의 구현](#이벤트드리븐-아키텍쳐의-구현)
    - [Poliglot](#폴리글랏-퍼시스턴스)
    - [Gateway](#Gateway)   
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-/-서킷-브레이킹-/-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [Persistence Volume](#Persistence-Volume)  [ConfigMap](#ConfigMap)
    - [Self_healing (liveness probe)](#Self_healing-(liveness-probe))
    - [무정지 재배포](#무정지-재배포)


# 서비스 시나리오

기능적 요구사항

1. 고객이 책을 선택하여 주문을 한다
2. 고객이 결제한다
3. 결제가 완료되면 주문내역이 서점으로 전달된다
4. 서점에서 주문내역을 확인하여 배송을 한다
5. 고객이 주문을 취소할 수 있다
6. 주문이 취소되면 결제가 취소된다
7. 결제가 취소되면 배송이 취소된다
8. 고객이 주문상태를 중간중간 조회한다
9. 주문상태가 바뀔 때 마다 카톡으로 알림을 보낸다

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
* MSAEZ 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/pYauKq27pAMMO4ZZcMLRDtjzgIv1/430f0486a27a998b5ae250808a899a95


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

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)
![image](https://user-images.githubusercontent.com/81279673/122707951-05d98180-d296-11eb-85d6-632551763d1c.png)

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)
![image](https://user-images.githubusercontent.com/81279673/122708047-33262f80-d296-11eb-95e0-7d8ea49b507e.png)

### 완성된 1차 모형
![image](https://user-images.githubusercontent.com/81279673/122315667-76f6fd00-cf55-11eb-8d7c-aef0294c98af.png)

    - View Model 추가
    - 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어)인 영어로 변경
    - 도메인 서열 분리 
        - Core Domain:  app, store : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기 : app 1주일 1회 미만, store 1개월 1회 미만
        - Supporting Domain: customerCenter : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기 : 1주일 1회 이상을 기준 ( 각팀 배포 주기 Policy 적용 )
        - General Domain:   pay : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음    
        

### 기능적 요구사항 검증
![image](https://user-images.githubusercontent.com/81279673/122316109-3cda2b00-cf56-11eb-85a2-f4dfd24973d0.png)

    - 고객이 책을 선택하여 주문을 한다 (ok)
    - 고객이 결제한다 (ok)
    - 결제가 완료되면 주문내역이 서점으로 전달된다 (ok)    
    - 서점에서 주문내역을 확인하여 배송을 한다 (ok)    

![image](https://user-images.githubusercontent.com/81279673/122317341-541a1800-cf58-11eb-8ff9-df3b85667a1d.png)

    - 고객이 주문을 취소할 수 있다 (ok)
    - 주문이 취소되면 결제가 취소된다 (ok)
    - 결제가 취소되면 배송이 취소된다 (ok)
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
![image](https://user-images.githubusercontent.com/81279673/123042198-6650f600-d431-11eb-8492-2bef51719d0f.png)

    - 수정된 모델은 모든 요구사항을 커버함


## 헥사고날 아키텍처 다이어그램 도출
![image](https://user-images.githubusercontent.com/81279673/122865863-07707b80-d362-11eb-9cf8-fe072d8518b0.png)

    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐
    - 고객센터(CustomerCenter)의 경우 Polyglot 적용을 위해 Hsql로 설계

# 구현

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 Bounded Context별로 대변되는 마이크로서비스들을 스프링부트로 구현하였다. 
- 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다. 

```
cd app 
mvn spring-boot:run 
포트 : 8081 

cd pay
mvn spring-boot:run 
포트 : 8082

cd store
mvn spring-boot:run  
포트 : 8083

cd customerCenter
mvn spring-boot:run 
포트 : 8084
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다. 

> Order.java 구현 내용
```java
package bookstore;

import javax.persistence.*;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String bookName;
    private Integer qty;
    private Integer price;
    private String status;
    private String userName;

    @PostPersist
    public void onPostPersist(){

        setStatus("Ordered");

        // 주문내역 Pub
        Ordered ordered = new Ordered();
        ordered.setId(this.getId());
        ordered.setBookName(this.getBookName());
        ordered.setQty(this.getQty());
        ordered.setPrice(this.getPrice());
        ordered.setStatus("Ordered");
        ordered.publishAfterCommit();

        // pay request
        bookstore.external.Pay pay = new bookstore.external.Pay();

        pay.setOrderId(this.getId());
        pay.setQty(this.getQty());
        pay.setPrice(this.getPrice());
        pay.setStatus("Paid");

        AppApplication.applicationContext.getBean(bookstore.external.PayService.class).pay(pay);

    }

    @PreRemove
    public void onPreRemove(){

        OrderCancelled orderCancelled = new OrderCancelled();
        orderCancelled.setId(this.getId());
        orderCancelled.setBookName(this.getBookName());
        orderCancelled.setQty(this.getQty());
        orderCancelled.setPrice(this.getPrice());
        orderCancelled.setStatus("OrderCancelled");    
        orderCancelled.publishAfterCommit();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 기반의 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리 없이 
  데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다.

> AppRepository.java 구현 내용
```java
package bookstore;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="orders", path="orders")
public interface OrderRepository extends PagingAndSortingRepository<Order, Long>{

}
```

- 적용 후 REST API 의 테스트
```
# app 서비스의 주문 신청
http POST http://localhost:8081/orders bookName=MASTERY qty=1 price=21000

# app 서비스의 주문 취소
http DELETE http://localhost:8081/orders/1 

# customercenter 서비스의 주문 상태 조회
http GET http://localhost:8084/orderStatusViews

# app 서비스의 주문 신청
http POST http://localhost:8081/orders bookName=SUMMER qty=1 price=17000

# app 서비스의 주문 조회
http GET http://localhost:8081/orders

# pay 서비스의 결제 조회
http GET http://localhost:8082/pays

# store 서비스의 배송 조회
http GET http://localhost:8082/pays
```
> 주문 신청 및 취소 결과 조회 (주문, 결제, 배송)
![image](https://user-images.githubusercontent.com/81279673/122730146-dede7800-d2b4-11eb-852b-15f984be4024.png)
![image](https://user-images.githubusercontent.com/81279673/122730257-fae21980-d2b4-11eb-82a2-122ce1b20b7c.png)
![image](https://user-images.githubusercontent.com/81279673/122730316-0d5c5300-d2b5-11eb-8696-b18773a8de5a.png)


## 동기식 호출 과 Fallback 처리

분석단계에서의 비기능 요구사항 중 하나로 결제처리가 되지 않으면 주문신청이 되지 않도록 동기식 호출을 통한 트랜잭션으로 처리하기로 하였다. 
호출 프로토콜은 이미 앞서 Rest Repository에 의해 노출되어 있는 REST 서비스를 FeignClient를 이용하여 호출하였다. 

- 결제(Pay)서비스를 호출하기 위하여 FeignClient를 활용하여 Service 대행 인터페이스(Proxy)를 구현하였다. 
> (app) external\PayService.java
```java
package bookstore.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="pay", url="${api.pay.url}")
public interface PayService {

    @RequestMapping(method= RequestMethod.POST, path="/pays")
    public void pay(@RequestBody Pay pay);

}
```
- 주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
> (app) Order.java (Entity)
```java
    public void onPostPersist(){

        setStatus("Ordered");

        // 주문내역 Pub
        Ordered ordered = new Ordered();
        ordered.setId(this.getId());
        ordered.setBookName(this.getBookName());
        ordered.setQty(this.getQty());
        ordered.setPrice(this.getPrice());
        ordered.setStatus("Ordered");
        ordered.publishAfterCommit();

        // pay request
        bookstore.external.Pay pay = new bookstore.external.Pay();

        pay.setOrderId(this.getId());
        pay.setQty(this.getQty());
        pay.setPrice(this.getPrice());
        pay.setStatus("Paid");

        AppApplication.applicationContext.getBean(bookstore.external.PayService.class).pay(pay);
    }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제시스템이 장애가 나면 주문도 못받는다는 것을 확인
```
# 결제 (pay) 서비스를 잠시 내려놓음

# 주문 처리
http POST http://localhost:8081/orders bookName=MASTERY qty=1 price=21000    #Fail
```
![image](https://user-images.githubusercontent.com/81279673/122716723-979cbb00-d2a5-11eb-8d40-3690618fd539.png)
```
# 결제 (pay) 서비스 재기동
cd pay
mvn spring-boot:run

#주문처리
http POST http://localhost:8081/orders bookName=MASTERY qty=1 price=21000   #Success
```
![image](https://user-images.githubusercontent.com/81279673/122716935-ddf21a00-d2a5-11eb-8bbd-fc4fd30f84ed.png)

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)


# 이벤트 드리븐 아키텍쳐의 구현

## 비동기식 호출

결제가 완료된 후 서점관리(store)로 이를 알려주는 행위는 동기식이 아닌 비동기식으로 처리하며, 서점관리 서비스의 처리를 위하여 결제가 블로킹 되지 않도록 처리하였다.
 
- 아래는 결제됨 이벤트를 카프카로 송출(Publish)하는 구현내용이다.
> (pay) Pay.java (Entity)
```java
package bookstore;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Pay_table")
public class Pay {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private String bookName;
    private Integer qty;
    private Integer price;
    private String status;

    @PostPersist
    public void onPostPersist(){
        Paid paid = new Paid();
        BeanUtils.copyProperties(this, paid);
        paid.publishAfterCommit();
    }
    
```
- 서점관리 서비스에서는 결제완료 이벤트를 수신하여 자신의 정책을 처리하도록 PolicyHandler를 구현하였다.
```java
package bookstore;

import bookstore.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaid_Delivery(@Payload Paid paid){
        
        if(!paid.validate()) return;

        Delivery delivery= new Delivery();
        delivery.setOrderId(paid.getOrderId());
        delivery.setPayId(paid.getId());
        delivery.setStatus("DeliveryStarted");        
        deliveryRepository.save(delivery);      
            
    }

```

## 시간적 디커플링 / 장애격리
서점관리(store) 시스템은 주문/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되므로 서점관리 시스템이 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없도록 구현하였다.
```
# 서점관리(store) 시스템을 잠시 내려놓음

# 주문 처리
http POST http://localhost:8081/orders bookName=SUMMER qty=1 price=17000   #Success
```
![image](https://user-images.githubusercontent.com/81279673/123035695-b37b9a80-d426-11eb-9c2a-ffeac70fd68b.png)

- 결제서비스가 정상적으로 조회되었는지 확인
```
http GET http://localhost:8082/pays   #Success
```
![image](https://user-images.githubusercontent.com/81279673/123035626-947d0880-d426-11eb-80a4-e2794250c1d3.png)

- 신청 상태 변경없음 확인
```
http GET http://localhost:8084/orderStatusView
```
![image](https://user-images.githubusercontent.com/81279673/123035752-d443f000-d426-11eb-9b66-04d02003ec36.png)

- 서점관리 서비스 재기동
```
cd store
mvn spring-boot:run
```
- 기동 후 주문상태 확인
```
http GET http://localhost:8081/orders   
```
![image](https://user-images.githubusercontent.com/81279673/123035841-09e8d900-d427-11eb-97fc-bfb21c57f0a2.png)
```
http GET http://localhost:8083/deliveries    
```
![image](https://user-images.githubusercontent.com/81279673/123035891-1f5e0300-d427-11eb-8270-d31abd082263.png)


## Correlation
MSAez 모델링 도구를 활용하여 각 서비스의 이벤트와 폴리시간의 연결을 pub/sub 점선으로 표현하였으며, 이를 코드로 자동생성하여 Correlation-key 연결을 구현하였다.
- 서점관리(store)시스템에서 상태가 배송시작으로 변경되면 주문(app)시스템 원천데이터의 상태(status) 정보가 update된다.  

> (app) PolicyHandler.java
```
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryStarted_StatusUpdate(@Payload DeliveryStarted deliveryStarted){

        if(!deliveryStarted.validate()) return;

        System.out.println("\n\n##### listener StatusUpdate : " + deliveryStarted.toJson() + "\n\n");       

        // Correlation id = 'orderId' 
        orderRepository.findById(Long.valueOf(deliveryStarted.getOrderId())).ifPresent(Match -> {

            System.out.println("##### wheneverDeliveryStarted_StatusUpdate.findById : exist");

            System.out.println("deliveryStarted 주문 번호 : "+ deliveryStarted.getOrderId());

            Order order = new Order();
            
            order.setId(deliveryStarted.getOrderId());
            order.setStatus(deliveryStarted.getStatus());
            System.out.println("\n\n##### wheneverDeliveryStarted_StatusUpdate order ID: " + order.getId() + "\n\n");
            
            orderRepository.save(order);
        });
           
    }
```

## CQRS
Materialized View 구현을 통해 다른 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이)도 내 서비스의 화면 구성과 잦은 조회가 가능하도록 구현하였다.
- 주문상태조회(orderStatusView)로 주문(order), 결제(pay), 배송(delivery) 상태를 고객이 언제든지 조회할 수 있도록 CQRS로 구현하였다.
- 발행된 이벤트 기반으로 Kafka를 통해 수신된 데이터를 별도 테이블에 적재하여 성능 Issue를 사전에 예방할 수 있다.
```
# customercenter 서비스의 주문 상태 조회 
http GET http://localhost:8084/orderStatusViews
```
> 주문신청 및 취소 후 주문상태 조회
![image](https://user-images.githubusercontent.com/81279673/122730545-4c8aa400-d2b5-11eb-888b-cd2344899d3e.png)


## 폴리글랏 퍼시스턴스
CustomerCenter의 경우 H2 DB인 App/Pay/Store 서비스와 다르게 Hsql로 구현했으며, 서로 다른 종류의 DB에도 문제없이 동작하여 다형성을 만족하도록 구현하였다.

> app, pay, store 서비스의 pom.xml 설정
```xml
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
```
> customerCenter 서비스의 pom.xml 설정
```xml
    <dependency>
        <groupId>org.hsqldb</groupId>
        <artifactId>hsqldb</artifactId>
        <scope>runtime</scope>
    </dependency>``
```

## Gateway 적용
API Gateway를 통하여 마이크로서비스들의 진입점을 단일화하였다.
> gateway > application.xml 설정
```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: app
          uri: http://localhost:8081
          predicates:
            - Path=/orders/** 
        - id: pay
          uri: http://localhost:8082
          predicates:
            - Path=/pays/** 
        - id: store
          uri: http://localhost:8083
          predicates:
            - Path=/deliveries/** 
        - id: customerCenter
          uri: http://localhost:8084
          predicates:
            - Path= /oderStatusViews/**
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


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: app
          uri: http://app:8080
          predicates:
            - Path=/orders/** 
        - id: pay
          uri: http://pay:8080
          predicates:
            - Path=/pays/** 
        - id: store
          uri: http://store:8080
          predicates:
            - Path=/deliveries/** 
        - id: customerCenter
          uri: http://customerCenter:8080
          predicates:
            - Path= /oderStatusViews/**
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
- Gateway 서비스 실행 상태에서 8088과 8081로 각각 order 서비스를 실행하였을 때 동일한 결과가 출력되었다.
```
http POST http://localhost:8081/orders bookName=SUMMER qty=1 price=17000
```
![image](https://user-images.githubusercontent.com/81279673/123205777-76caa480-d4f5-11eb-856e-e949da482b6f.png)
```
http POST http://localhost:8088/orders bookName=SUMMER qty=1 price=17000
```
![image](https://user-images.githubusercontent.com/81279673/123205929-b5f8f580-d4f5-11eb-8f44-327d340c86b2.png)
```
http GET http://localhost:8081/orders
```
![image](https://user-images.githubusercontent.com/81279673/123205827-88ac4780-d4f5-11eb-9f83-1d90e361dcd1.png)
```
http GET http://localhost:8088/orders
```
![image](https://user-images.githubusercontent.com/81279673/123205862-9792fa00-d4f5-11eb-8b3b-dfee2c8938fb.png)


## CI/CD 설정
각 구현체들은 각각의 source repository 에 구성되었고, 소스코드 빌드 및 패키징/도커라이징/deploy 및 서비스 생성을 진행하였다.

- git에서 소스 가져오기
```
git clone https://github.com/sebastiaok/bookstore.git
```
- 소스코드 빌드, 패키징
```
cd bookstore
cd app
mvn package
```
- 도커라이징 : Azure 레지스트리에 도커 이미지 푸시하기
```
az acr build --registry skccuser05 --image skccuser03.azurecr.io/app:latest .
```
- 컨테이너라이징 : 디플로이 생성 확인
```
kubectl create deploy app --image=skccuser03.azurecr.io/app:latest
```
- 컨테이너라이징 : 서비스 생성
```
kubectl expose deploy app --port=8080
```
- pay, store, customercenter, gateway 서비스도 동일한 작업 진행

> 결과확인
![image](https://user-images.githubusercontent.com/81279673/123206966-8ea32800-d4f7-11eb-8de4-6e338add1b81.png)


## 동기식 호출 / 서킷 브레이킹 / 장애격리
서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
시나리오는 매칭요청(match)-->결제(payment) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB 를 통하여 장애격리.


1. Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
- application.yml

![Hystrix설정](https://user-images.githubusercontent.com/75401933/105256018-35417080-5bc8-11eb-9c55-dec189b1bed5.png)


2. 피호출 서비스(결제:payment) 의 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게

- (payment) Payment.java (Entity)

![image](https://user-images.githubusercontent.com/75401933/105285536-f3302300-5bf7-11eb-8227-9f43f68b6287.png)

3. istio설정

- virtualservice.yaml 생성

![image](https://user-images.githubusercontent.com/75401933/105285074-fe368380-5bf6-11eb-92f9-df8af52f1158.png)

3. 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
 - 동시사용자 100명
 - 60초 동안 실시
 
```
 - siege -c100 -t60S -r5 -v --content-type "application/json" 'http://match:8080/matches POST {"id": 600, "price":1000, "status": "matchRequest"}' 
```
![siege](https://user-images.githubusercontent.com/75401933/105274031-bbb57c80-5bdf-11eb-8e24-b349b79a5f80.png)

서킷브레이크가 발생하지 않아 아래와 같이 여러 조건으로 부하테스트를 진행하였으나, 500 에러를 발견할 수 없었음

```
 - siege -c255 -t1M -r5 -v --content-type "application/json" 'http://match:8080/matches POST {"id": 600, "price":1000, "status": "matchRequest"}' 
 
 - siege -c255 -t2M -r5 -v --content-type "application/json" 'http://match:8080/matches POST {"id": 600, "price":1000, "status": "matchRequest"}' 
```


## 오토스케일 아웃

앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다.
match구현체에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 10프로를 넘어서면 replica 를 10개까지 늘려준다:

- autosclae 적용

```
kubectl autoscale deployment match --cpu-percent=10 --min=1 --max=10
```
![AutoScaling1](https://user-images.githubusercontent.com/75401933/105279069-279ce280-5bea-11eb-9efd-a1b310cfd75b.png)

```
kubectl exec -it pod/siege -- /bin/bash
siege -c100 -t30S -v --content-type "application/json" 'http://52.231.52.214:8080/matches POST  {"id": 1000, "price":1000, "status": "matchRequest", "student":"testStudent"}'
```
부하에 따라 visit pod의 cpu 사용률이 증가했고, Pod Replica 수가 증가하는 것을 확인할 수 있었음

![AutoScaling](https://user-images.githubusercontent.com/75401933/105278740-75651b00-5be9-11eb-9f06-11253eea34d6.png)

## Persistence Volume

visit 컨테이너를 마이크로서비스로 배포하면서 영속성 있는 저장장치(Persistent Volume)를 적용함

• PVC 설정 확인

![pvcDescribe](https://user-images.githubusercontent.com/75401933/105258282-be5aa680-5bcc-11eb-86c7-531f48900c57.png)


• PVC Volume설정 확인
visit 구현체에서 해당 pvc를 volumeMount 하여 사용 

```
kubectl get pod visit -o yaml
```
![PVC볼륨설정확인](https://user-images.githubusercontent.com/75401933/105261676-34faa280-5bd3-11eb-8a7c-aa27b73b95a7.png)

- visit pod에 접속하여 mount 용량 확인

![image](https://user-images.githubusercontent.com/75401933/105268535-c3702380-5bd5-11eb-933e-a82b92e90f0b.png)


## Self_healing (liveness probe)
mypage구현체의 deployment.yaml 소스 서비스포트를 8080이 아닌 고의로 8081로 변경하여 재배포한 후 pod 상태 확인

- 정상/비정상 pod 정보 조회

![image](https://user-images.githubusercontent.com/75401933/105279506-08528500-5beb-11eb-89a0-346481020201.png)


## 무정지 재배포
클러스터 배포 시 readinessProbe 설정이 없으면 다운타임이 존재하게 된다. 무정지 재배포가 100% 되는 것인지 확인하기 위해서 readinessProbe 옵션을 삭제/원복 후 테스트를 하였다. 배포시 다운타임의 존재 여부를 확인하기 위하여, siege 라는 부하 테스트 툴을 사용한다. (Availability 체크)

1. readinessProbe가 없는 상태에서 배포 진행

- readiness 옵션 제거
> (app) kubernetes/deployment.yml
```yaml
  replicas: 1
  selector:
    matchLabels:
      app: app
  template:
    metadata:
      labels:
        app: app
    spec:
      containers:
        - name: app
          image: skccuser03.azurecr.io/app:latest
          ports:
            - containerPort: 8080
#          readinessProbe:
#           httpGet:
#              path: '/actuator/health'
#              port: 8080
#            initialDelaySeconds: 10
#            timeoutSeconds: 2
#            periodSeconds: 5
#            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```
- 설정 삭제 확인
```yaml
kubectl get deploy app -o yaml
```
![image](https://user-images.githubusercontent.com/81279673/123229494-514b9400-d511-11eb-9a9e-4915c4aefe2d.png)
- siege 부하테스트 툴을 사용하여 Availability 확인
```
# 충분한 시간만큼 부하를 준다
siege -c1 -t60S -v http://app:8080/orders --delay=1S

# 신규버전 배포
kubectl set image deploy app app=skccuser03.azurecr.io/app:v1
```
- seige 화면에서 Availability 가 100% 미만으로 떨어졌는지 확인
![image](https://user-images.githubusercontent.com/81279673/123224253-9de0a080-d50c-11eb-9abf-88fd353d8194.png)

2. readinessProbe 설정 후 새로운 배포 진행
```
kubectl get deploy app -o yaml
```
![image](https://user-images.githubusercontent.com/81279673/123225350-98378a80-d50d-11eb-99af-3c2849562605.png)

- 동일한 시나리오로 재배포 한 후 Availability 확인
- 배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인
![image](https://user-images.githubusercontent.com/81279673/123226593-c4074000-d50e-11eb-87a9-58514c61fc70.png)




