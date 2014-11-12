/****************************************************************************
**
** Copyright (C) 2013 Digia Plc and/or its subsidiary(-ies).
** Contact: http://www.qt-project.org/legal
**
** This file is part of the examples of the Qt Toolkit.
**
** $QT_BEGIN_LICENSE:BSD$
** You may use this file under the terms of the BSD license as follows:
**
** "Redistribution and use in source and binary forms, with or without
** modification, are permitted provided that the following conditions are
** met:
**   * Redistributions of source code must retain the above copyright
**     notice, this list of conditions and the following disclaimer.
**   * Redistributions in binary form must reproduce the above copyright
**     notice, this list of conditions and the following disclaimer in
**     the documentation and/or other materials provided with the
**     distribution.
**   * Neither the name of Digia Plc and its Subsidiary(-ies) nor the names
**     of its contributors may be used to endorse or promote products derived
**     from this software without specific prior written permission.
**
**
** THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
** "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
** LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
** A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
** OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
** SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
** LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
** DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
** THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
** (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
** OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
**
** $QT_END_LICENSE$
**
****************************************************************************/

#include <QtWidgets>
#include <QRegExp>
#include "mainwindow.h"
#include "xbeltree.h"
void MainWindow::PathChange(QString newpath)
{
//    qDebug()<<"pathchage";
    pathshow->setText(newpath);
}
void MainWindow::FileCopy(){
    copiedpath=pathshow->text();
    qDebug()<<copiedpath+" copied";
}
void MainWindow::FilePaste(){
    QRegExp rx("(.*)/(.*)");
    QRegExp rx2("(.*)/");
    int pos,pos2;
    QString filename,path;
    pos=copiedpath.indexOf(rx);
    pos2=pathshow->text().indexOf(rx2);
    filename=(rx.cap(2));
    path=(rx2.cap(0));
    qDebug()<<"filename:"<<filename;
    qDebug()<<"cp "+copiedpath+" "+path+filename;
}
void MainWindow::FileDelete(){
    qDebug()<<"rm " +pathshow->text();
}
void MainWindow::FileTouch(){
    int pos2;
    QString path;
    QRegExp rx2("(.*)/");
    pos2=pathshow->text().indexOf(rx2);
    path=(rx2.cap(0));
    qDebug()<<"touch " + path + newfilename->text() + " " + size->text();
}
MainWindow::MainWindow()
{
    xbelTree = new XbelTree;
    QHBoxLayout *leftLayout = new QHBoxLayout;
    leftLayout->addWidget(xbelTree);
    connect(xbelTree, SIGNAL(itemChanged(QTreeWidgetItem*,int)), this, SLOT(defaultsave()));
    connect(xbelTree, SIGNAL(pathchange(QString)), this, SLOT(PathChange(QString)));
    QVBoxLayout *rightLayout = new QVBoxLayout;
    pathshow = new QLineEdit("Hello World!",this);
    newfilename = new QLineEdit("FileName");
    size = new QLineEdit("FileSize");
    QAbstractButton *but_delete=new  QPushButton("Delete");
    QAbstractButton *but_touch=new  QPushButton("Touch");
    QAbstractButton *but_copy=new  QPushButton("Copy");
    QAbstractButton *but_paste=new  QPushButton("Paste");
    connect(but_copy,SIGNAL(clicked()),this,SLOT(FileCopy()));
    connect(but_touch,SIGNAL(clicked()),this,SLOT(FileTouch()));
    connect(but_paste,SIGNAL(clicked()),this,SLOT(FilePaste()));
    connect(but_delete,SIGNAL(clicked()),this,SLOT(FileDelete()));
    rightLayout->addWidget(newfilename);
    rightLayout->addWidget(size);
    rightLayout->addWidget(but_touch);
    rightLayout->addWidget(but_delete);
    rightLayout->addWidget(but_copy);
    rightLayout->addWidget(but_paste);
    rightLayout->addStretch();
    QHBoxLayout *viewLayout = new QHBoxLayout;
    viewLayout->addLayout(leftLayout);
    viewLayout->addLayout(rightLayout);
    viewLayout->setStretch(0, 3);
    viewLayout->setStretch(1, 1);
    QVBoxLayout *mainLayout = new QVBoxLayout;
    mainLayout->addLayout(viewLayout);
    mainLayout->addWidget(pathshow);
    QWidget *mainWidget = new QWidget;
    mainWidget->setLayout(mainLayout);
    setCentralWidget(mainWidget);

    createActions();
    createMenus();

    statusBar()->showMessage(tr("Ready"));

    setWindowTitle(tr("DOM Bookmarks"));
    resize(480, 320);
}
void MainWindow::defaultopen()
{
    QString fileName ="/users/zyhc/desktop/test.xbel";
    if (fileName.isEmpty())
        return;

    QFile file(fileName);
    if (!file.open(QFile::ReadOnly | QFile::Text)) {
        QMessageBox::warning(this, tr("SAX Bookmarks"),
                             tr("Cannot read file %1:\n%2.")
                             .arg(fileName)
                             .arg(file.errorString()));
        return;
    }

    if (xbelTree->read(&file))
        statusBar()->showMessage(tr("File loaded"), 2000);
}
void MainWindow::defaultsave()
{
    QString fileName ="/users/zyhc/desktop/test.xbel";
    if (fileName.isEmpty())
        return;

    QFile file(fileName);
    if (!file.open(QFile::WriteOnly | QFile::Text)) {
        QMessageBox::warning(this, tr("SAX Bookmarks"),
                             tr("Cannot write file %1:\n%2.")
                             .arg(fileName)
                             .arg(file.errorString()));
        return;
    }

    if (xbelTree->write(&file))
        statusBar()->showMessage(tr("File saved"), 2000);
}
void MainWindow::open()
{
    QString fileName =
            QFileDialog::getOpenFileName(this, tr("Open Bookmark File"),
                                         QDir::currentPath(),
                                         tr("XBEL Files (*.xbel *.xml)"));
    if (fileName.isEmpty())
        return;

    QFile file(fileName);
    if (!file.open(QFile::ReadOnly | QFile::Text)) {
        QMessageBox::warning(this, tr("SAX Bookmarks"),
                             tr("Cannot read file %1:\n%2.")
                             .arg(fileName)
                             .arg(file.errorString()));
        return;
    }

    if (xbelTree->read(&file))
        statusBar()->showMessage(tr("File loaded"), 2000);
}

void MainWindow::saveAs()
{
    QString fileName =
            QFileDialog::getSaveFileName(this, tr("Save Bookmark File"),
                                         QDir::currentPath(),
                                         tr("XBEL Files (*.xbel *.xml)"));
    if (fileName.isEmpty())
        return;

    QFile file(fileName);
    if (!file.open(QFile::WriteOnly | QFile::Text)) {
        QMessageBox::warning(this, tr("SAX Bookmarks"),
                             tr("Cannot write file %1:\n%2.")
                             .arg(fileName)
                             .arg(file.errorString()));
        return;
    }

    if (xbelTree->write(&file))
        statusBar()->showMessage(tr("File saved"), 2000);
}

void MainWindow::about()
{
   QMessageBox::about(this, tr("About DOM Bookmarks"),
                      tr("The <b>DOM Bookmarks</b> example demonstrates how to "
                         "use Qt's DOM classes to read and write XML "
                         "documents."));
}

void MainWindow::createActions()
{
    openAct = new QAction(tr("&Open..."), this);
    openAct->setShortcuts(QKeySequence::Open);
    connect(openAct, SIGNAL(triggered()), this, SLOT(open()));

    saveAsAct = new QAction(tr("&Save As..."), this);
    saveAsAct->setShortcuts(QKeySequence::SaveAs);
    connect(saveAsAct, SIGNAL(triggered()), this, SLOT(saveAs()));

    exitAct = new QAction(tr("E&xit"), this);
    exitAct->setShortcuts(QKeySequence::Quit);
    connect(exitAct, SIGNAL(triggered()), this, SLOT(close()));

    aboutAct = new QAction(tr("&About"), this);
    connect(aboutAct, SIGNAL(triggered()), this, SLOT(about()));

    aboutQtAct = new QAction(tr("About &Qt"), this);
    connect(aboutQtAct, SIGNAL(triggered()), qApp, SLOT(aboutQt()));
}

void MainWindow::createMenus()
{
    fileMenu = menuBar()->addMenu(tr("&File"));
    fileMenu->addAction(openAct);
    fileMenu->addAction(saveAsAct);
    fileMenu->addAction(exitAct);

    menuBar()->addSeparator();

    helpMenu = menuBar()->addMenu(tr("&Help"));
    helpMenu->addAction(aboutAct);
    helpMenu->addAction(aboutQtAct);
}
