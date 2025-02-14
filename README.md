server:
  port: 9998
  servlet:
    session:
      timeout: 36000s
  error:
    include-exception: true # 오류 응답에 exception의 내용을 포함할지 여부
    include-stacktrace: always # 오류 응답에 stacktrace 내용을 포함할 지 여부


spring:
  session:
    store-type: none
    jdbc:
      initialize-schema: never
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        globally_quoted_identifiers: true
        dialect: "org.hibernate.dialect.MySQL8Dialect"
    defer-datasource-initialization: false

  datasource:
    hikari:
      connection-timeout: 30000
      max-lifetime: 59000
    driver-class-name: net.sf.log4jdbc.sql.jdbcapi.DriverSpy
    url: jdbc:log4jdbc:mysql://localhost:3306/db_micehubsales?zeroDateTimeBehavior=convertToNull
    username: root
    password: pass

  thymeleaf:
    cache: false

  security:
    debug: false

  sql:
    init:
      mode: never

  output:
    ansi:
      enabled: always

  # 파일업로드
  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB
      max-request-size: 55MB

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.micehub.sales.mapper

# aws IAM user (albatross 계정사용)
pagehelper:
  helper-dialect: mysql
  reasonable: true
  supportMethodsArguments: true



cloud:
  aws:
    region:
      static: ap-northeast-2
    credentials:
      accessKey: 
      secretKey: 
    sqs:
      queueUrl: 
    ses:
      region: ap-northeast-2
      configSet: 
    sns:
      topicArn: 
oauth:
  server:
    url: "http://localhost:9977"
  client:
    id: 
    secret: 
    grantType: "client_credentials"
    scope: "read"



# 커스텀 설정ㄴ
micehubsales:
  schedule:
    cronEmail: 0 0/1 * * * ?
    cronDelete: 0 0/3 * * * ?
    use: false
  damo:
    encrypt: false
  sms:
    test: true
  email:
    test: true
    testReceiverEmail: nick1961@micehub.com
    emiceHomepageUrl: http://localhost:9998
    offerSenderEmail : mkt@micehub.com
    error-report: false
  file:
    upload-dir: /Users/kimnamgyu/upload_files
    template-dir: /docs/templates
    excel-dir: /docs/excelform
  image:
    path: localhost:9998

# POPBiLL SDK 설정
popbill:
  linkId: micehub
  secretKey: none
  isTest: true
  isIpRestrictOnOff: true
  useStaticIp: false
  useLocalTimeYn: true


springdoc:
  api-docs:
    enabled: true
