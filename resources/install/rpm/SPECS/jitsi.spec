Name:     jitsi
Version:  1.1
Release:  4467.10442
Summary:  Jitsi - Open Source Video Calls and Chat
Packager: Damian Minkov <damencho@jitsi.org>

Group:     Applications/Internet
License:   GNU Lesser General Public License
URL:       https://www.jitsi.org
Source0:   http://download.jitsi.org/jitsi/nightly/src/jitsi-src-1.1-4467-10442.zip
BuildRoot: %{_topdir}/buildroot

AutoReqProv:   no
BuildRequires: java-devel >= 0:1.5.0
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: gzip
BuildRequires: subversion

Requires: java >= 0:1.5.0

%description
Jitsi is an audio/video Internet phone and instant messenger that
supports some of the most popular instant messaging and telephony protocols
such as SIP, Jabber, AIM/ICQ, MSN, Yahoo! Messenger, Bonjour, RSS and
counting. Jitsi is completely Open Source / Free Software, and is
freely available under the terms of the GNU Lesser General Public License.

%prep
%setup -q -n jitsi

%build
ant -Dlabel=4467.10442 rebuild

%install
[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

mkdir -p $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/usr
mkdir -p $RPM_BUILD_ROOT/usr/bin
mkdir -p $RPM_BUILD_ROOT/usr/share
mkdir -p $RPM_BUILD_ROOT/usr/share/applications
mkdir -p $RPM_BUILD_ROOT/usr/share/doc/jitsi
mkdir -p $RPM_BUILD_ROOT/usr/share/man/man1
mkdir -p $RPM_BUILD_ROOT/usr/share/pixmaps
mkdir -p $RPM_BUILD_ROOT/usr/share/jitsi
mkdir -p $RPM_BUILD_ROOT/usr/share/jitsi/lib
mkdir -p $RPM_BUILD_ROOT/usr/share/jitsi/lib/bundle
mkdir -p $RPM_BUILD_ROOT/usr/share/jitsi/lib/native
mkdir -p $RPM_BUILD_ROOT/usr/share/jitsi/sc-bundles

# copy the documentation
cp resources/install/debian/jitsi.1.tmpl $RPM_BUILD_ROOT/usr/share/man/man1/jitsi.1
sed -i -e "s/_PACKAGE_NAME_/jitsi/"  $RPM_BUILD_ROOT/usr/share/man/man1/jitsi.1
sed -i -e "s/_APP_NAME_/Jitsi/"  $RPM_BUILD_ROOT/usr/share/man/man1/jitsi.1
gzip $RPM_BUILD_ROOT/usr/share/man/man1/jitsi.1

# copy the launcher script
cp resources/install/debian/jitsi.sh.tmpl $RPM_BUILD_ROOT/usr/bin/jitsi
sed -i -e "s/_PACKAGE_NAME_/jitsi/" $RPM_BUILD_ROOT/usr/bin/jitsi

# no more libaoss
#sed -i -e "s@export LD_PRELOAD=/usr/lib/libaoss.so@export LD_PRELOAD=/usr/lib/libaoss.so.0@" $RPM_BUILD_ROOT/usr/bin/sip-communicator
chmod a+x $RPM_BUILD_ROOT/usr/bin/jitsi

# copy the menu icons
cp resources/install/debian/jitsi-32.xpm $RPM_BUILD_ROOT/usr/share/pixmaps/jitsi-32.xpm
cp resources/install/debian/jitsi-16.xpm $RPM_BUILD_ROOT/usr/share/pixmaps/jitsi-16.xpm
cp resources/install/debian/jitsi.svg $RPM_BUILD_ROOT/usr/share/pixmaps/jitsi.svg

# copy the menu entry
cp resources/install/debian/jitsi.desktop.tmpl $RPM_BUILD_ROOT/usr/share/applications/jitsi.desktop
sed -i -e "s/_PACKAGE_NAME_/jitsi/"  $RPM_BUILD_ROOT/usr/share/applications/jitsi.desktop
sed -i -e "s/_APP_NAME_/Jitsi/"      $RPM_BUILD_ROOT/usr/share/applications/jitsi.desktop

# copy the sc-bundles
cp sc-bundles/*.jar $RPM_BUILD_ROOT/usr/share/jitsi/sc-bundles/
# remove all slicks
rm -rf $RPM_BUILD_ROOT/usr/share/jitsi/sc-bundles/*-slick.jar
rm -rf $RPM_BUILD_ROOT/usr/share/jitsi/sc-bundles/slick*.jar

# copy the os-specific sc-bundles
cp sc-bundles/os-specific/linux/*.jar $RPM_BUILD_ROOT/usr/share/jitsi/sc-bundles/

# copy the lib jars
cp lib/*.jar $RPM_BUILD_ROOT/usr/share/jitsi/lib/
cp lib/bundle/* $RPM_BUILD_ROOT/usr/share/jitsi/lib/bundle/
rm $RPM_BUILD_ROOT/usr/share/jitsi/lib/bundle/junit.jar
cp lib/os-specific/linux/*.jar $RPM_BUILD_ROOT/usr/share/jitsi/lib/

# copy the native libs
%ifarch i386 i586 i686
cp lib/native/linux/* $RPM_BUILD_ROOT/usr/share/jitsi/lib/native/
%else
cp lib/native/linux-64/* $RPM_BUILD_ROOT/usr/share/jitsi/lib/native/
%endif

# copy the resources
cp resources/install/logging.properties $RPM_BUILD_ROOT/usr/share/jitsi/lib/
cp lib/felix.client.run.properties $RPM_BUILD_ROOT/usr/share/jitsi/lib/

# Make felix deploy its bundles in ~/.felix/sip-communicator.bin
sed -i -e "s/felix.cache.profiledir=sip-communicator.bin/felix.cache.profile=sip-communicator.bin/" $RPM_BUILD_ROOT/usr/share/jitsi/lib/felix.client.run.properties
# fix shell script
sed -i -e "s/\/usr\/lib\/jni/\$LIBPATH\/native/" $RPM_BUILD_ROOT/usr/bin/jitsi
sed -i -e "s/\/usr\/share\/java\/jdic\.jar/\$LIBPATH\/jdic_stub\.jar:\$LIBPATH\/jdic-all\.jar/" $RPM_BUILD_ROOT/usr/bin/jitsi
sed -i -e "s/\/usr\/share\/java\/org\.apache\.felix\.framework\.jar/\$LIBPATH\/felix\.jar/" $RPM_BUILD_ROOT/usr/bin/jitsi
sed -i -e "s/\/usr\/share\/java\/org\.apache\.felix\.main\.jar://" $RPM_BUILD_ROOT/usr/bin/jitsi
sed -i -e "s/\/launchutils.jar//" $RPM_BUILD_ROOT/usr/bin/jitsi

%clean
[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%{_bindir}/jitsi
#%{_libdir}/jitsi/
/usr/share/jitsi
/usr/share/applications/jitsi*
/usr/share/pixmaps/jitsi*
%doc %{_mandir}/man*/*

%changelog
* Mon Mar 11 2013 Pavel Tankov <ptankov@bluejimp.com>
- Now depends on java >= 0:1.5.0.

* Thu Jan 31 2013 Damian Minkov <damencho@jitsi.org>
- Fixed startup script. 
- Add felix.framework and felix.main dependencies.
- Fix warning about conflicting folders with filesystem package.

* Thu Mar 23 2011 Pavel Tankov <tankov_pavel@yahoo.com>
- Renamed to the new project name -jitsi

* Mon Apr 19 2010 Pavel Tankov <tankov_pavel@yahoo.com>
- Now depends on java >= 1:1.5.0.

* Wed Mar 31 2010 Pavel Tankov <tankov_pavel@yahoo.com>
- Handled the manpages.

* Tue Mar 30 2010 Pavel Tankov <tankov_pavel@yahoo.com>
- Migrated the build process on a Fedora 12 x86_64 machine. It used to be a
  Debian which, after a distupgrade, couldn't run rpmbuild properly anymore.
- Took out the svn update and ant rebuild actions and put them in the external
  script that calls rpmbuild with this spec.
- Updated the description section.

* Tue Dec 18 2007 Pavel Tankov <tankov_pavel@yahoo.com>
- Put SC bundles and libraries under /usr/lib instead of /usr/share
- Changed BuildPrereq to subversion instead of cvs
- Changed the "Source:" tag to reflect the new location of the last nightly build
- Patched the launcher script so that LD_PRELOAD points to /usr/lib/libaoss.so.0
  instead of /usr/lib/libaoss.so

* Fri Feb 23 2007 Pavel Tankov <tankov_pavel@yahoo.com>
- Fixed to reflect the new guidelines for the layout
  on http://www.sip-communicator.org/index.php/Documentation/HowToBuildAnInstaller
- Removed the folder /usr/share/sip-communicator/lib/os-cpecific
  because it was not needed.
- Added stuff from sc-bundles/os-specific/linux/ because it was missing.
- This fix resulted in the systray icon showing now.

* Thu Feb 15 2007 Pavel Tankov <tankov_pavel@yahoo.com>
- Fixed to reflect the new images in $RPM_BUILD_ROOT/usr/share/pixmaps/
- TODO: incorporate the systray icon.

* Sat Jan 27 2007 Pavel Tankov <tankov_pavel@yahoo.com>
- Removed /usr/share/menu because it was not needed.
- Fixed to reflect the new directory structure with the "os-specific"
  and "installer-exclude" folders in mind.
- TODO: handle manpages.
- TODO: check whether user has java installed.

* Mon Jan 08 2007 Pavel Tankov <tankov_pavel@yahoo.com>
- Initial RPM release.
