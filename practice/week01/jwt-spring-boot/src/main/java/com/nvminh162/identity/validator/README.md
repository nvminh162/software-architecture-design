Tôi sẽ trace từng dòng code chi tiết khi có request đến với dữ liệu invalid:

## **Luồng chạy chi tiết từng dòng code:**

### **Bước 1: Request đến Controller**
```
POST /users
Body: { "username": "test", "password": "12345678", "dob": "2015-01-01" }
```


**File**: `UserController.java`
```java
@PostMapping
ResponseEntity<ApiResponse<UserResponse>> createUser(
    @Valid @RequestBody UserCreationRequest request) // ← @Valid trigger validation
```


---

### **Bước 2: Spring validation framework khởi tạo**

**File**: `DobValidator.java:24`
```java
@Override
public void initialize(DobConstraint constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    min = constraintAnnotation.min(); // ← Đọc min=18 từ annotation
}
```

**Dữ liệu**: `this.min = 18`

---

### **Bước 3: Thực hiện validation**

**File**: `DobValidator.java:14`
```java
@Override
public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
    if (Objects.isNull(value))  // value = 2015-01-01, not null
        return true;
    
    long years = ChronoUnit.YEARS.between(value, LocalDate.now());
    // years = between(2015-01-01, 2026-01-17) = 11
    
    return years >= min; // 11 >= 18 → FALSE
}
```

**Kết quả**: `false` → Validation FAIL

---

### **Bước 4: Spring ném MethodArgumentNotValidException**

Spring framework tạo exception với:
- `FieldError` chứa `defaultMessage = "INVALID_DOB"`
- `ConstraintViolation` chứa annotation attributes

---

### **Bước 5: GlobalExceptionHandler bắt exception**

**File**: `GlobalExceptionHandler.java:56`

**Dòng 57**: Lấy message code
```java
String enumKey = ex.getFieldError().getDefaultMessage();
// enumKey = "INVALID_DOB"
```


**Dòng 58**: Khởi tạo default errorCode
```java
ErrorCode errorCode = ErrorCode.INVALID_KEY;
```


**Dòng 60**: Khởi tạo attributes
```java
Map<String, Object> attributes = null;
```


**Dòng 62-68**: Try-catch block
```java
try {
    // Dòng 63: Convert string → ErrorCode enum
    errorCode = ErrorCode.valueOf(enumKey);
    // errorCode = ErrorCode.INVALID_DOB
    
    // Dòng 64: Lấy ConstraintViolation
    var contraintViolation = ex.getBindingResult()
                               .getAllErrors()
                               .getFirst()
                               .unwrap(ConstraintViolation.class);
    
    // Dòng 65: Trích xuất attributes từ annotation
    attributes = contraintViolation.getConstraintDescriptor().getAttributes();
    /* attributes = {
         "min": 18,
         "message": "INVALID_DOB",
         "groups": [],
         "payload": []
       }
    */
    
    // Dòng 66: Log attributes
    log.info(attributes.toString());
} catch (IllegalArgumentException e) {
    // Không xảy ra vì "INVALID_DOB" tồn tại trong ErrorCode enum
}
```


**Dòng 70-73**: Build response
```java
ApiResponse<?> response = ApiResponse.builder()
    .code(errorCode.getCode()) // 1003
    .message(Objects.nonNull(attributes) 
        ? mapAttribute(errorCode.getMessage(), attributes) 
        : errorCode.getMessage())
    .build();
```


---

### **Bước 6: mapAttribute() format message**

**File**: `GlobalExceptionHandler.java:77`

**Giả sử ErrorCode.INVALID_DOB.getMessage() = "Invalid date of birth. The minimum age is {min}"**

```java
private String mapAttribute(String message, Map<String, Object> attributes) {
    // Dòng 78: Lấy giá trị min
    String minValue = attributes.get(MIN_ATTRIBUTE).toString();
    // minValue = "18"
    
    // Dòng 79: Thay thế placeholder
    return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    // "Invalid date of birth. The minimum age is {min}"
    // → "Invalid date of birth. The minimum age is 18"
}
```


---

### **Bước 7: Trả response về client**

**File**: `GlobalExceptionHandler.java:74`
```java
return ResponseEntity.status(errorCode.getStatusCode()).body(response);
```


**Response**:
```json
{
  "code": 1003,
  "message": "Invalid date of birth. The minimum age is 18"
}
```


---

## **Tổng kết các file được đi qua theo thứ tự:**

1. `UserController.java` - Nhận request
2. `DobValidator.java:24` - initialize() gán min=18
3. `DobValidator.java:14` - isValid() kiểm tra và return false
4. `GlobalExceptionHandler.java:56` - handleMethodArgumentNotValidException()
5. `GlobalExceptionHandler.java:77` - mapAttribute() format message
6. Response trả về client