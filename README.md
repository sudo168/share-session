# share-session

功能强大的session会话管理工具包

完全实现 HttpSession 功能，同时提供SessionUtils工具类。用于在没有HttpServletRequest情况下
直接通过id对session进行操作。 实现 app、web、pc、web动静分离 等 会话统一管理。

使用习惯仍然是HttpSession

# 环境要求

1. 目前仅支持 springboot
2. Servlet 3.0 以上  (changeSessionId方法)

# 配置使用

## 1. 导入jar包

在springboot工程pom中引入依赖: （可能需要自己下载源码打包）

        <dependency>
            <groupId>net.ewant</groupId>
            <artifactId>share-session</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

## 2. 实现 redis持久化 接口
       
2.1 内置有redis持久化默认实现，如果要默认实现则需要引入工程 redis-helper（配置参看redis-helper工程这里不敷述）

        <dependency>
            <groupId>net.ewant</groupId>
            <artifactId>redis-helper</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

2.2 如果自己实现session持久存储，则 自己写一个类继承 AbstractSessionDao，然后将该类交给spring管理即可，框架自己会主动寻找使用

## 3. application.yml 或者 properties中配置

在server节点下

server:

    port: 8090
    context-path: /
    connection-timeout: 20000
    session:
        timeout: 5
        token-parameter-name: token
        token-header-name: Authorization
        cookie:
          name: JSESSIONID

# 【demo】

@Controller

public class DemoController {

    @Autowired
    HttpServletRequest httpRequest;

    @RequestMapping(value = "/demo")
    @ResponseBody
    public String demo(HttpServletRequest request, HttpSession session){// 参数里以及上面注入的HttpServletRequest，均是自己包装过的。因此可以自定义操作
    
        //HttpSession session = request.getSession();// 这里拿到的是自己实现的HttpSession子类，方法参数中的session也是
        
        System.out.println(request.getParameter("a"));
        System.out.println(httpRequest.getParameter("a"));

        session.setAttribute("a", "123");// 里面会调用我们配置SessionDao去存储

        return "demo";
    }
}

#  从 HttpServletRequest 中获取jsessionid/token, HttpSession 的过程

1. 先从请求cookies中获取名称为JSESSIONID（可自定义，遵循ServletSessionConfig配置）的cookie作为session id
 
2. 如果拿不到，紧接着从request中通过request.getParameter, 获取名为 token-parameter-name（自己配置，不配默认是token）的请求参数作为session id

3. 如果还拿不到，则调用 request.getHeader , 获取名为 token-header-name（自己配置，不配默认是Authorization）的请求参数作为session id

4. 如果拿到，则通过id获取session

5. 以上均拿不到，生成新的HttpSession（有新的id）
