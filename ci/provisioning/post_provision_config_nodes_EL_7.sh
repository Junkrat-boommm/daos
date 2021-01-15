#!/bin/bash

url_to_repo() {
    local URL="$1"

    local repo=${URL#*://}
    repo="${repo//%252F/_}"
    repo="${repo//\//_}"

    echo "$repo"
}

disable_gpg_check() {
    local REPO="$1"

    dnf config-manager --save --setopt="$(url_to_repo "$REPO")".gpgcheck=0
}

post_provision_config_nodes() {

    # Reserve port ranges 31416-31516 for DAOS and CART servers
    echo 31416-31516 > /proc/sys/net/ipv4/ip_local_reserved_ports

    if $CONFIG_POWER_ONLY; then
        rm -f /etc/dnf.repos.d/*.hpdd.intel.com_job_daos-stack_job_*_job_*.repo
        dnf -y erase fio fuse ior-hpc mpich-autoload               \
                     ompi argobots cart daos daos-client dpdk      \
                     fuse-libs libisa-l libpmemobj mercury mpich   \
                     openpa pmix protobuf-c spdk libfabric libpmem \
                     libpmemblk munge-libs munge slurm             \
                     slurm-example-configs slurmctld slurm-slurmmd
    fi

    local dnf_repo_args="--disablerepo=*"

    if [ -n "$DAOS_STACK_GROUP_REPO" ]; then
         rm -f /etc/dnf.repos.d/*"$DAOS_STACK_GROUP_REPO"
         dnf config-manager \
             --add-repo="${REPOSITORY_URL}${DAOS_STACK_GROUP_REPO}"
    fi

    if [ -n "$DAOS_STACK_LOCAL_REPO" ]; then
        rm -f /etc/dnf.repos.d/*"$DAOS_STACK_LOCAL_REPO"
        local repo="${REPOSITORY_URL}${DAOS_STACK_LOCAL_REPO}"
        dnf config-manager --add-repo="${repo}"
        disable_gpg_check "$repo"
    fi

    # TODO: this should be per repo for the above two repos
    dnf_repo_args+=" --enablerepo=repo.dc.hpdd.intel.com_repository_*"

    if [ -n "$INST_REPOS" ]; then
        for repo in $INST_REPOS; do
            branch="master"
            build_number="lastSuccessfulBuild"
            if [[ $repo = *@* ]]; then
                branch="${repo#*@}"
                repo="${repo%@*}"
                if [[ $branch = *:* ]]; then
                    build_number="${branch#*:}"
                    branch="${branch%:*}"
                fi
            fi
            local repo="${JENKINS_URL}"job/daos-stack/job/"${repo}"/job/"${branch//\//%252F}"/"${build_number}"/artifact/artifacts/centos7/
            dnf config-manager --add-repo="${repo}"
            disable_gpg_check "$repo"
            # TODO: this should be per repo in the above loop
            if [ -n "$INST_REPOS" ]; then
                dnf_repo_args+=",build.hpdd.intel.com_job_daos-stack*"
            fi
        done
    fi
    if [ -n "$INST_RPMS" ]; then
        # shellcheck disable=SC2086
        dnf -y erase $INST_RPMS
    fi
    rm -f /etc/profile.d/openmpi.sh
    rm -f /tmp/daos_control.log
    dnf -y install redhat-lsb-core
    # shellcheck disable=SC2086
    if [ -n "$INST_RPMS" ] &&
       ! dnf -y $dnf_repo_args install $INST_RPMS; then
        rc=${PIPESTATUS[0]}
        for file in /etc/dnf.repos.d/*.repo; do
            echo "---- $file ----"
            cat "$file"
        done
        exit "$rc"
    fi
    if [ ! -e /usr/bin/pip3 ] &&
       [ -e /usr/bin/pip3.6 ]; then
        ln -s pip3.6 /usr/bin/pip3
    fi
    if [ ! -e /usr/bin/python3 ] &&
       [ -e /usr/bin/python3.6 ]; then
        ln -s python3.6 /usr/bin/python3
    fi
    # install the debuginfo repo in case we get segfaults
    cat <<"EOF" > /etc/dnf.repos.d/CentOS-Debuginfo.repo
[core-0-debuginfo]
name=CentOS-7 - Debuginfo
baseurl=http://debuginfo.centos.org/7/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-Debug-7
enabled=0
EOF

    # now make sure everything is fully up-to-date
    if ! time dnf -y upgrade \
                  --exclude fuse,mercury,daos,daos-\*; then
        exit 1
    fi

    exit 0
}
