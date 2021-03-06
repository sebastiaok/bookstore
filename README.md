# Online Bookstore

본 과제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성하였습니다.  
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 개인과제 수행 결과입니다.


- 체크포인트 : https://workflowy.com/s/assessment/qJn45fBdVZn4atl3



# Table of contents

- [예제 - 온라인 서점](#---)

  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계) 
  - [구현](#구현)
    - [DDD 의 적용](#DDD-의-적용)
    - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
    - [이벤트드리븐 아키텍쳐의 구현](#이벤트드리븐-아키텍쳐의-구현)
    - [시간적 디커플링 / 장애격리](#시간적-디커플링-/-장애격리)
    - [Correlation](#Correlation)
    - [CQRS](#CQRS)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [Gateway](#Gateway)   
  - [운영](#운영)
    - [CI/CD 설정](#CI/CD-설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-/-서킷-브레이킹-/-장애격리)
    - [오토스케일아웃](#오토스케일아웃)
    - [Persistence Volume](#Persistence-Volume) 
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
- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW
1. Saga
1. CQRS
1. Correlation
1. Req/Resp
1. Gateway
1. Deploy/ Pipeline
1. Circuit Breaker
1. Autoscale (HPA)
1. Zero-downtime deploy (Readiness Probe)
1. Config Map/ Persistence Volume
1. Polyglot
1. Self-healing (Liveness Probe)
    

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


## 동기식 호출과 Fallback 처리

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

- 결제서비스가 정상적으로 처리되었는지 확인
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

## Gateway
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
Spring FeignClient + Hystrix을 사용하여 서킷 브레이킹을 구현하였다.
주문(app)-->결제(pay) 시 RESTful Request/Response로 되어있고, 결제 요청이 과도할 경우 CB를 통하여 장애격리 가능하도록 설정하였다.

- 결제를 요청하는 주문(app) 서비스에서 Hystrix 설정 - 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
> (app) application.yml 파일
```yaml
feign:
  hystrix:
    enabled: true
hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

- 결제 서비스(pay)에서 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하도록 설정
> (pay) Pay.java
```java
    @PostPersist
    public void onPostPersist(){
        try{
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e){
           e.printStackTrace();
        }

        String status = this.getStatus();

        if (status.equals("Paid")) {
            
            Paid paid = new Paid();
            paid.setId(this.getId());
            paid.setOrderId(this.getOrderId());
            paid.setBookName(this.getBookName());
            paid.setQty(this.getQty());
            paid.setPrice(this.getPrice());
            paid.setStatus("Paid");
            paid.publishAfterCommit();
```

- 부하테스터 siege 툴을 통한 서킷브레이커 동작 확인:
    - 동시사용자 100명
    - 60초 동안 실시
```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://app:8080/orders POST {"bookName":"SUMMER", "qty":1, "price":21000}'
```
- 부하가 발생하고 서킷브레이커가 발동하여 요청 실패하였고, 밀린 부하가 다시 처리되면서 주문신청이 진행됨
![image](https://user-images.githubusercontent.com/81279673/123304233-aca17480-d559-11eb-8c13-25aa611ee99c.png)
![image](https://user-images.githubusercontent.com/81279673/123304528-00ac5900-d55a-11eb-98df-891630a37ff5.png)


## 오토스케일아웃
앞서 서킷브레이커는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 하였다.

- 부하가 제대로 걸리기 위해서, 서비스의 리소스를 줄여서 재배포한다.
> (app) deployment.yaml
```yaml
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m  
```
- 신청서비스에 대한 replica를 동적으로 늘려주도록 HPA를 설정한다. 설정은 CPU 사용량이 15% 를 넘어서면 replica를 10개까지 늘려준다.
```
kubectl autoscale deploy app --min=1 --max=10 --cpu-percent=15
```
![image](https://user-images.githubusercontent.com/81279673/123308586-d0b38480-d55e-11eb-96e5-c0459908ebc3.png)

- CB 에서 했던 방식대로 워크로드를 1분 동안 걸어준다.
```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://app:8080/orders POST {"bookName":"SUMMER", "qty":1, "price":21000}'
```
- 오토스케일 모니터링을 걸어 스케일 아웃이 자동으로 진행됨을 확인한다.
```
kubectl get deploy app -w
```
![image](https://user-images.githubusercontent.com/81279673/123310331-ef1a7f80-d560-11eb-9c36-0b9efba8bf1b.png)
- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지고, Pod Replica 수가 증가하는 것을 확인할 수 있다.
![image](https://user-images.githubusercontent.com/81279673/123310787-70721200-d561-11eb-8517-d1bd81aed2a9.png)
![image](https://user-images.githubusercontent.com/81279673/123311317-fee69380-d561-11eb-9007-40eeeafc19a5.png)


## Persistence Volume
store 서비스가 PVC(PersistentVolumeClaim)를 사용하도록 설정하였다.

- PVC(PersistentVolumeClaim) 생성
> volume-pvc.yaml
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: azure-managed-disk
spec:
  accessModes:
  - ReadWriteOnce
  storageClassName: default
  resources:
    requests:
      storage: 1Gi
```
- PVC 설정 확인
![image](https://user-images.githubusercontent.com/81279673/123236993-3cbeca00-d518-11eb-9869-9bd8030c82c9.png)
![image](https://user-images.githubusercontent.com/81279673/123237222-6ed02c00-d518-11eb-97ea-0e74bc3b6216.png)

- store 서비스에서 해당 pvc를 volumeMount 하여 사용 
```
kubectl get pod store -o yaml
```
![image](https://user-images.githubusercontent.com/81279673/123258702-9e8a2e80-d52e-11eb-99b6-1bfbcf2fe7ef.png)

- store pod에 접속하여 마운트 용량 확인
- 마운트된 경로에 파일을 생성하고 다른 pod에서 파일 확인
![image](https://user-images.githubusercontent.com/81279673/123262298-b2379400-d532-11eb-87a7-9e2d42cc244f.png)
![image](https://user-images.githubusercontent.com/81279673/123262081-76043380-d532-11eb-92e3-fedbb6c987e5.png)


## Self_healing (liveness probe)
Self-healing 확인을 위해 Liveness Probe 포트를 8080이 아닌 8081로 변경하여 재배포한 후 pod 상태를 확인하였다.
- Liveness Probe 옵션 변경
> (app) kubernetes/deployment.yml
```yaml
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8081
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```
- app pod에 Liveness Probe 옵션 적용 확인
```
kubectl describe pod app-7447857c97-655sw
```
![image](https://user-images.githubusercontent.com/81279673/123283736-a1911900-d546-11eb-9487-2b073227abe9.png)

- app pod 적용 시 retry 발생 확인

![image](https://user-images.githubusercontent.com/81279673/123282597-a0132100-d545-11eb-9084-c6f9ee4a1b8b.png)
![image](https://user-images.githubusercontent.com/81279673/123282651-adc8a680-d545-11eb-97ad-8d31d9956560.png)


## 무정지 재배포
클러스터 배포 시 readinessProbe 설정이 없으면 다운타임이 존재하게 된다. 무정지 재배포가 100% 되는 것인지 확인하기 위해서 readinessProbe 옵션을 삭제/원복 후 테스트를 하였다. 배포시 다운타임의 존재 여부를 확인하기 위하여, siege 라는 부하 테스트 툴을 사용한다. (Availability 체크하여 어느정도의 실패가 있었는지 확인)

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




