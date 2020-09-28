sudo echo "* soft nofile 655350" >> /etc/security/limits.conf
sudo echo "* hard nofile 655350" >> /etc/security/limits.conf
sudo echo "* hard nproc 65535000" >> /etc/security/limits.conf
sudo echo "* soft nproc 65535000" >> /etc/security/limits.conf
sudo echo "* soft stack 524288000" >> /etc/security/limits.conf
sudo echo "* hard stack 524288000" >> /etc/security/limits.conf

sudo echo "session required /lib/security/pam_limits.so" >> /etc/pam.d/login
