
# Plan: Enable Stub File Loading for Ownership Checking

## Problem Analysis

The test `BorrowTests.java` fails with `method.invocation` errors at lines 27-28 when calling `toString()` and `length()` on `@Borrowed StringBuilder` parameters.

**The stub file has correct annotations** (already verified):
- Line 140: `String toString(@Borrowed StringBuilder this) {};`
- Line 154: `int length(@Borrowed AbstractStringBuilder this);`

**Root Cause: Stub file is not being loaded.** Two issues:

1. **Missing `@StubFiles` annotation**: `HemileiaChecker` doesn't have the `@StubFiles("jdk.astub")` annotation to tell the Checker Framework to load the stub file.

2. **Maven resources not including .astub files**: The pom.xml resources configuration (lines 120-126) only includes `**/*.properties` from `src/main/java`:
  ```xml                                                                                                                                                                                                                            
  <includes>                                                                                                                                                                                                                        
  <include>**/*.properties</include>                                                                                                                                                                                                
  </includes>                                                                                                                                                                                                                       
  ```                                                                                                                                                                                                                               
The `.astub` file is not being copied to the build output, so even with `@StubFiles`, it wouldn't be found at runtime.

## Solution

### File 1: `src/main/java/name/mateusborges/checker/HemileiaChecker.java`

Add the `@StubFiles` annotation to load `jdk.astub`:

  ```java                                                                                                                                                                                                                           
  import org.checkerframework.framework.qual.StubFiles;                                                                                                                                                                             
                                                                                                                                                                                                                                    
  @StubFiles("jdk.astub")                                                                                                                                                                                                           
  @RelevantJavaTypes(Object.class)                                                                                                                                                                                                  
  public class HemileiaChecker extends BaseTypeChecker {                                                                                                                                                                            
  // ...                                                                                                                                                                                                                            
  }                                                                                                                                                                                                                                 
  ```                                                                                                                                                                                                                               

Per [Checker Framework documentation](https://checkerframework.org/api/org/checkerframework/framework/qual/StubFiles.html), stub files listed in `@StubFiles` must be in the same directory as the checker class.

### File 2: `pom.xml`

Update the resources configuration to include `.astub` files:

  ```xml                                                                                                                                                                                                                            
  <resources>                                                                                                                                                                                                                       
  <resource>                                                                                                                                                                                                                        
  <directory>src/main/java</directory>                                                                                                                                                                                              
  <includes>                                                                                                                                                                                                                        
  <include>**/*.properties</include>                                                                                                                                                                                                
  <include>**/*.astub</include>                                                                                                                                                                                                     
  </includes>                                                                                                                                                                                                                       
  </resource>                                                                                                                                                                                                                       
  <resource>                                                                                                                                                                                                                        
  <directory>src/main/resources</directory>                                                                                                                                                                                         
  </resource>                                                                                                                                                                                                                       
  </resources>                                                                                                                                                                                                                      
  ```                                                                                                                                                                                                                               

## Verification

1. Run `./mvnw clean test -Dtest=HemileiaCheckerTest`
2. Expected: `BorrowTests.java` lines 27-28 should no longer produce `method.invocation` errors
3. The test should pass with all 9 expected diagnostics found and 0 unexpected diagnostics     