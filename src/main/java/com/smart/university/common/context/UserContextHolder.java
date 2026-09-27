package com.smart.university.common.context;

/**
 * 用户上下文持有者，基于 ThreadLocal 在当前请求线程内透传登录身份
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> USER_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /**
     * 设置当前登录用户上下文
     *
     * @param userContext 用户上下文
     */
    public static void setUserContext(UserContext userContext) {
        USER_CONTEXT_THREAD_LOCAL.set(userContext);
    }

    /**
     * 获取当前登录用户上下文
     *
     * @return 用户上下文
     */
    public static UserContext getUserContext() {
        return USER_CONTEXT_THREAD_LOCAL.get();
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID
     */
    public static Long getUserId() {
        UserContext result = getUserContext();
        return result == null ? null : result.getUserId();
    }

    /**
     * 获取当前登录学生 ID
     *
     * @return 学生 ID
     */
    public static Long getStudentId() {
        UserContext result = getUserContext();
        return result == null ? null : result.getStudentId();
    }

    /**
     * 获取当前登录教师 ID
     *
     * @return 教师 ID
     */
    public static Long getTeacherId() {
        UserContext result = getUserContext();
        return result == null ? null : result.getTeacherId();
    }

    /**
     * 清理当前线程的用户上下文，避免线程池复用导致身份串号
     */
    public static void clear() {
        USER_CONTEXT_THREAD_LOCAL.remove();
    }
}
