package com.jcuke.generator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.file.PsiDirectoryImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/6/14.
 */
public class jpaDaoGenerator extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        VirtualFile root = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        VirtualFileFilter filter = VirtualFileFilter.ALL;
        final List<VirtualFile> targetFolders = new ArrayList<VirtualFile>();
        ContentIterator iterator = new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile virtualFile) {
                String vfpath = virtualFile.getPath();
                if (virtualFile.getName().equals("jpadao") && virtualFile.isDirectory()) {
                    System.out.println(vfpath);
                    if (vfpath.contains("jpadao")) {
                        targetFolders.add(virtualFile);
                    }
                }
                return true;
            }
        };
        VfsUtilCore.iterateChildrenRecursively(root, filter, iterator);

        iterator = new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile virtualFile) {
                String vfpath = virtualFile.getPath();
                if (virtualFile.getName().endsWith(".java") && !virtualFile.isDirectory()) {
                    System.out.println(vfpath);
                    if (vfpath.contains("com") && vfpath.contains("entity")) {
                        PsiManagerImpl pmi = (PsiManagerImpl) PsiManagerImpl.getInstance(project);
                        PsiDirectory dir = new PsiDirectoryImpl(pmi, targetFolders.get(0));
                        String targetClassName = virtualFile.getName().replaceFirst(".java", "");
                        targetClassName += "Dao";
                        final PsiJavaFile psiJavaFile = (PsiJavaFile) dir.findFile(targetClassName + ".java");
                        if(psiJavaFile != null){
                            //jpa dao 接口已经存在, 则直接删除文件, 接下来会重新创建
                            //psiJavaFile.delete();
                            //这里直接删除会报错
                            //ERROR - plication.impl.ApplicationImpl - Assertion failed: Write access is allowed inside write-action only (see com.intellij.openapi.application.Application.runWriteAction())
                            //所以用这个方法
                            new WriteCommandAction.Simple(project, psiJavaFile) {

                                @Override
                                protected void run() throws Throwable {

                                    psiJavaFile.delete();

                                }
                            }.execute();
                        }
                        PsiClass pc = JavaDirectoryService.getInstance().createInterface(dir, targetClassName);
                        System.out.println(pc.getText());
                        System.out.println(pc);
                    }
                }
                return true;
            }
        };
        VfsUtilCore.iterateChildrenRecursively(root, filter, iterator);

        iterator = new ContentIterator() {
            @Override
            public boolean processFile(final VirtualFile virtualFile) {
                if (virtualFile.getName().endsWith("Dao.java") && !virtualFile.isDirectory()) {
                    try {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                        new WriteCommandAction.Simple(project, psiFile) {
                            @Override
                            protected void run() throws Throwable {
                                virtualFile.setBinaryContent(getClassText(virtualFile));

                            }
                        }.execute();
                    } catch (Throwable e){
                        e.printStackTrace();
                    }
                }
                return true;
            }
        };
        VfsUtilCore.iterateChildrenRecursively(root, filter, iterator);

    }

    public byte[] getClassText(VirtualFile virtualFile) {

        String template = "package PACKAGENAME;" +
                "\n\n" +
                "import IMPORTNAME;\n" +
                "import org.springframework.data.jpa.repository.JpaRepository;\n" +
                "\n" +
                "/**\n" +
                " * @program: matrix-live\n" +
                " * @description: the description of ENTITYNAME jpa dao\n" +
                " * @author: waangjinwen's idea plgin \n" +
                " */\n" +
                "public interface ENTITYNAMEDao extends JpaRepository<ENTITYNAME, Integer> {\n" +
                "\n" +
                "}";

        System.out.println(virtualFile.getPath());
        System.out.println(virtualFile.getParent().getPath());

        String packageName = virtualFile.getParent().getPath().substring(virtualFile.getParent().getPath().indexOf("com/")).replaceAll("/", ".");
        String entityName = virtualFile.getPath().substring(virtualFile.getPath().lastIndexOf("/") + 1, virtualFile.getPath().lastIndexOf(".java")).replaceAll("Dao", "");
        String importName = packageName.substring(0, packageName.lastIndexOf(".") + 1) + "entity." + entityName;

        String text = template.replaceAll("PACKAGENAME", packageName).replaceAll("IMPORTNAME", importName).replaceAll("ENTITYNAME", entityName);
        return text.getBytes();
    }

    public void retrive(PsiElement element) {
        for (PsiElement psiElement : element.getChildren()) {
            System.out.println(psiElement.toString());
            if (psiElement.getContext() != null) {
                System.out.println(psiElement.getContext().getText());
            }
        }
    }

}
