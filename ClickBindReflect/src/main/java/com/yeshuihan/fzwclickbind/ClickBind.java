package com.yeshuihan.fzwclickbind;

import android.app.Activity;
import android.view.View;

import com.yeshuihan.clickbindannotation.Click;
import com.yeshuihan.clickbindannotation.LongClick;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;



public class ClickBind {
    /**
     * 直接反射调用
     * @param activity
     */
    public static void bind(Activity activity) {
        bindClick(activity);
        bindLongClick(activity);
    }

    /**
     * 反射绑定生成的辅助类，
     * @param activity
     */
    public static void bind2(Activity activity) {
        bindClick2(activity);
        bindLongClick2(activity);
    }


    private static void bindClick(Activity activity) {
        try {
            Method[] allMethod = activity.getClass().getDeclaredMethods();
            Method method;
            Click annotation;
            for (int i = 0; i < allMethod.length; i++) {
                method = allMethod[i];
                annotation = method.getAnnotation(Click.class);
                if (annotation != null) {
                    int id = annotation.value();
                    View view = activity.findViewById(id);
                    Method finalMethod = method;
                    finalMethod.setAccessible(true);
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                finalMethod.invoke(activity, v);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void bindLongClick(Activity activity) {
        try {
            Method[] allMethod = activity.getClass().getDeclaredMethods();
            Method method;
            LongClick annotation;
            for (int i = 0; i < allMethod.length; i++) {
                method = allMethod[i];
                annotation = method.getAnnotation(LongClick.class);
                if (annotation != null) {
                    int id = annotation.value();
                    View view = activity.findViewById(id);
                    Method finalMethod = method;
                    finalMethod.setAccessible(true);
                    view.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            try {
                                finalMethod.invoke(activity, v);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private static void bindClick2(Activity activity) {
        try {
            Class<?> classes = activity.getClassLoader().loadClass(activity.getClass().getName()+"ClickBind");
            Constructor<?> constructor = classes.getConstructor(activity.getClass());
            View.OnClickListener listener = (View.OnClickListener) constructor.newInstance(activity);

            Method[] allMethod = activity.getClass().getDeclaredMethods();
            Method method;
            Click annotation;
            for (int i = 0; i < allMethod.length; i++) {
                method = allMethod[i];
                annotation = method.getAnnotation(Click.class);
                if (annotation != null) {
                    int id = annotation.value();
                    View view = activity.findViewById(id);
                    view.setOnClickListener(listener);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void bindLongClick2(Activity activity) {
        try {
            Class<?> classes = activity.getClassLoader().loadClass(activity.getClass().getName()+"LongClickBind");
            Constructor<?> constructor = classes.getConstructor(activity.getClass());
            View.OnLongClickListener listener = (View.OnLongClickListener) constructor.newInstance(activity);

            Method[] allMethod = activity.getClass().getDeclaredMethods();
            Method method;
            LongClick annotation;
            for (int i = 0; i < allMethod.length; i++) {
                method = allMethod[i];
                annotation = method.getAnnotation(LongClick.class);
                if (annotation != null) {
                    int id = annotation.value();
                    View view = activity.findViewById(id);
                    view.setOnLongClickListener(listener);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
