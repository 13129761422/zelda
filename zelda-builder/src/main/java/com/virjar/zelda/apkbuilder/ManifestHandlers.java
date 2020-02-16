package com.virjar.zelda.apkbuilder;


import com.virjar.zelda.buildsrc.Constants;

import org.jf.pxb.android.axml.AxmlVisitor;
import org.jf.pxb.android.axml.NodeVisitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ManifestHandlers {


    /**
     * [INSTALL_FAILED_DUPLICATE_PERMISSION: Package virjar.zelda.comssandroidugca.xrvzNi attempting to redeclare permission com.ss.android.ugc.aweme.permission.MIPUSH_RECEIVE already owned by com.ss.android.ugc.aweme]
     */
    public static class ReNamePermissionDeclare extends AxmlVisitor {
        private ZeldaBuildContext zeldaBuildContext;

        public ReNamePermissionDeclare(AxmlVisitor av, ZeldaBuildContext zeldaBuildContext) {
            super(av);
            this.zeldaBuildContext = zeldaBuildContext;
        }

        @Override
        public NodeVisitor visitFirst(String namespace, String name) {

            return new NodeVisitor(super.visitFirst(namespace, name)) {


                @Override
                public NodeVisitor visitChild(String ns, String name) {// application
                    if (!"permission".equals(name)) {
                        return super.visitChild(ns, name);
                    }

                    return new NodeVisitor(super.visitChild(ns, name)) {

                        @Override
                        public void visitContentAttr(String ns, String name, int resourceId, int type, Object obj) {
                            if ("name".equals(name) && obj instanceof String && AxmlVisitor.NS_ANDROID.equals(ns)) {
                                String permissionDeclare = obj.toString();
                                obj = permissionDeclare + "." + zeldaBuildContext.sufferKey;
                            }

                            super.visitContentAttr(ns, name, resourceId, type, obj);
                        }
                    };
                }
            };
        }
    }

    public static class ReNameProviderAuthorities extends AxmlVisitor {
        private ZeldaBuildContext zeldaBuildContext;

        public ReNameProviderAuthorities(AxmlVisitor av, ZeldaBuildContext zeldaBuildContext) {
            super(av);
            this.zeldaBuildContext = zeldaBuildContext;
        }

        @Override
        public NodeVisitor visitFirst(String namespace, String name) {

            return new NodeVisitor(super.visitFirst(namespace, name)) {


                @Override
                public NodeVisitor visitChild(String ns, String name) {// application
                    if (!"application".equals(name)) {
                        return super.visitChild(ns, name);
                    }

                    return new NodeVisitor(super.visitChild(ns, name)) {

                        @Override
                        public NodeVisitor visitChild(String ns, String name) {
                            if (!"provider".equals(name)) {
                                return super.visitChild(ns, name);
                            }
                            return new NodeVisitor(super.visitChild(ns, name)) {
                                @Override
                                public void visitContentAttr(String ns, String name, int resourceId, int type, Object obj) {
                                    if ("authorities".equals(name) && obj instanceof String && AxmlVisitor.NS_ANDROID.equals(ns)) {
                                        String authorities = obj.toString();
                                        obj = authorities + zeldaBuildContext.sufferKey;
                                    }
                                    super.visitContentAttr(ns, name, resourceId, type, obj);
                                }
                            };
                        }
                    };
                }
            };
        }
    }

    public static class FixRelativeClassName extends AxmlVisitor {
        private static final Set<String> androidComponents = new HashSet<>(Arrays.asList("activity", "receiver", "service", "provider"));
        private ZeldaBuildContext zeldaBuildContext;

        public FixRelativeClassName(AxmlVisitor av, ZeldaBuildContext zeldaBuildContext) {
            super(av);
            this.zeldaBuildContext = zeldaBuildContext;
        }

        @Override
        public NodeVisitor visitFirst(String namespace, String name) {

            return new NodeVisitor(super.visitFirst(namespace, name)) {


                @Override
                public NodeVisitor visitChild(String ns, String name) {// application
                    if (!"application".equals(name)) {
                        return super.visitChild(ns, name);
                    }

                    return new NodeVisitor(super.visitChild(ns, name)) {

                        @Override
                        public NodeVisitor visitChild(String ns, String name) {
                            //activity receiver service provider
                            if (!androidComponents.contains(name)) {
                                return super.visitChild(ns, name);
                            }
                            return new NodeVisitor(super.visitChild(ns, name)) {
                                @Override
                                public void visitContentAttr(String ns, String name, int resourceId, int type, Object obj) {
                                    if ("name".equals(name) && obj instanceof String) {
                                        String componentName = obj.toString();
                                        if (componentName.startsWith(".")) {
                                            obj = zeldaBuildContext.apkMeta.getPackageName() + componentName;
                                        }
                                        zeldaBuildContext.declaredComponentClassNames.add(obj.toString());
                                    }
                                    super.visitContentAttr(ns, name, resourceId, type, obj);
                                }
                            };
                        }
                    };
                }
            };
        }

    }

    public static class EnableDebug extends AxmlVisitor {
        public EnableDebug(AxmlVisitor nv) {
            super(nv);
        }

        @Override
        public NodeVisitor visitFirst(String namespace, String name) {
            return new NodeVisitor(super.visitFirst(namespace, name)) {


                @Override
                public NodeVisitor visitChild(String ns, String name) {// application
                    if (!"application".equals(name)) {
                        return super.visitChild(ns, name);
                    }

                    return new NodeVisitor(super.visitChild(ns, name)) {

                        @Override
                        public void visitEnd() {
                            // android:debuggable(0x0101000f)=(type 0x12)0xffffffff
                            super.visitContentAttr(AxmlVisitor.NS_ANDROID, "debuggable", AxmlVisitor.DEBUG_RESOURCE_ID,
                                    TYPE_INT_BOOLEAN, Boolean.TRUE);
                            super.visitEnd();
                        }
                    };
                }
            };
        }
    }

    public static class ReplaceApplication extends AxmlVisitor {
        private ZeldaBuildContext zeldaBuildContext;

        public ReplaceApplication(AxmlVisitor av, ZeldaBuildContext zeldaBuildContext) {
            super(av);
            this.zeldaBuildContext = zeldaBuildContext;
        }

        @Override
        public NodeVisitor visitFirst(String namespace, String name) {
            return new NodeVisitor(super.visitFirst(namespace, name)) {
                @Override
                public void visitContentAttr(String ns, String name, int resourceId, int type, Object obj) {
                    if ("package".equals(name)) {
                        obj = zeldaBuildContext.newPkgName;
                    }
                    super.visitContentAttr(ns, name, resourceId, type, obj);
                }

                @Override
                public NodeVisitor visitChild(String ns, String name) {// application
                    if (!"application".equals(name)) {
                        return super.visitChild(ns, name);
                    }

                    return new NodeVisitor(super.visitChild(ns, name)) {

                        @Override
                        public void visitEnd() {
                            //替换 Application Name
                            super.visitContentAttr(AxmlVisitor.NS_ANDROID, "name", AxmlVisitor.NAME_RESOURCE_ID,
                                    AxmlVisitor.TYPE_STRING, Constants.ZeldaApplicationClassName);
                            super.visitEnd();
                        }
                    };
                }
            };
        }
    }
}