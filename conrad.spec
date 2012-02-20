%define __jar_repack %{nil}
%define debug_package %{nil}
%define __strip /bin/true
%define __os_install_post   /bin/true
%define __check_files /bin/true
Summary: conrad
Name: conrad
Version: 0.1.0
Release: 5
Epoch: 0
BuildArchitectures: noarch
Group: Applications
BuildRoot: %{_tmppath}/%{name}-%{version}-buildroot
License: BSD
Provides: conrad
Requires: iplant-service-config >= 0.1.0-4
Source0: %{name}-%{version}.tar.gz

%description
iPlant Conrad

%pre
getent group iplant > /dev/null || groupadd -r iplant
getent passwd iplant > /dev/null || useradd -r -g iplant -d /home/iplant -s /bin/bash -c "User for the iPlant services." iplant
exit 0

%prep
%setup -q
mkdir -p $RPM_BUILD_ROOT/etc/init.d/

%build
unset JAVA_OPTS
lein deps
lein uberjar

%install
install -d $RPM_BUILD_ROOT/usr/local/lib/conrad/
install -d $RPM_BUILD_ROOT/var/run/conrad/
install -d $RPM_BUILD_ROOT/var/lock/subsys/conrad/
install -d $RPM_BUILD_ROOT/var/log/conrad/
install -d $RPM_BUILD_ROOT/etc/conrad/

install conrad $RPM_BUILD_ROOT/etc/init.d/
install conrad-1.0.0-SNAPSHOT-standalone.jar $RPM_BUILD_ROOT/usr/local/lib/conrad/
install conf/log4j.properties $RPM_BUILD_ROOT/etc/conrad/

%post
/sbin/chkconfig --add conrad

%preun
if [ $1 -eq 0 ] ; then
	/sbin/service conrad stop >/dev/null 2>&1
	/sbin/chkconfig --del conrad
fi

%postun
if [ "$1" -ge "1" ] ; then
	/sbin/service conrad condrestart >/dev/null 2>&1 || :
fi

%clean
lein clean
rm -r lib/*
rm -r $RPM_BUILD_ROOT

%files
%attr(-,iplant,iplant) /usr/local/lib/conrad/
%attr(-,iplant,iplant) /var/run/conrad/
%attr(-,iplant,iplant) /var/lock/subsys/conrad/
%attr(-,iplant,iplant) /var/log/conrad/
%attr(-,iplant,iplant) /etc/conrad/

%config %attr(0644,iplant,iplant) /etc/conrad/log4j.properties
%config %attr(0644,iplant,iplant) /etc/conrad/conrad.properties

%attr(0755,root,root) /etc/init.d/conrad
%attr(0644,iplant,iplant) /usr/local/lib/conrad/conrad-1.0.0-SNAPSHOT-standalone.jar
