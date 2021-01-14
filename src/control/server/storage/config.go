//
// (C) Copyright 2019-2021 Intel Corporation.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// GOVERNMENT LICENSE RIGHTS-OPEN SOURCE SOFTWARE
// The Government's rights to use, modify, reproduce, release, perform, display,
// or disclose this software are subject to the terms of the Apache License as
// provided in Contract No. 8F-30005.
// Any reproduction of computer software, computer software documentation, or
// portions thereof marked with this legend must also reproduce the markings.
//

package storage

import "github.com/pkg/errors"

const (
	// MinNVMeStorage defines the minimum per-target allocation
	// that may be requested. Requests with smaller amounts will
	// be rounded up.
	MinNVMeStorage = 1 << 30 // 1GiB, from bio_xtream.c

	// MinScmToNVMeRatio defines the minimum-allowable ratio
	// of SCM to NVMe.
	MinScmToNVMeRatio = 0.01 // 1%
	// DefaultScmToNVMeRation defines the default ratio of
	// SCM to NVMe.
	DefaultScmToNVMeRatio = 0.06
)

const (
	maxScmDeviceLen = 1
)

const (
	ScmClassNone ScmClass = ""
	ScmClassDCPM ScmClass = "dcpm"
	ScmClassRAM  ScmClass = "ram"
)

// ScmClass specifies device type for Storage Class Memory
type ScmClass string

// UnmarshalYAML implements yaml.Unmarshaler on ScmClass type
func (s *ScmClass) UnmarshalYAML(unmarshal func(interface{}) error) error {
	var class string
	if err := unmarshal(&class); err != nil {
		return err
	}

	scmClass := ScmClass(class)
	switch scmClass {
	case ScmClassDCPM, ScmClassRAM:
		*s = scmClass
	default:
		return errors.Errorf("scm_class value %q not supported in config (dcpm/ram)", scmClass)
	}
	return nil
}

func (s ScmClass) String() string {
	return string(s)
}

// ScmConfig represents a SCM (Storage Class Memory) configuration entry.
type ScmConfig struct {
	MountPoint  string   `yaml:"scm_mount,omitempty" cmdLongFlag:"--storage" cmdShortFlag:"-s"`
	Class       ScmClass `yaml:"scm_class,omitempty"`
	RamdiskSize int      `yaml:"scm_size,omitempty"`
	DeviceList  []string `yaml:"scm_list,omitempty"`
}

func (sc *ScmConfig) Validate() error {
	if sc.MountPoint == "" {
		return errors.New("no scm_mount set")
	}

	switch sc.Class {
	case ScmClassDCPM:
		if sc.RamdiskSize > 0 {
			return errors.New("scm_size may not be set when scm_class is dcpm")
		}
		if len(sc.DeviceList) == 0 {
			return errors.New("scm_list must be set when scm_class is dcpm")
		}
	case ScmClassRAM:
		if sc.RamdiskSize == 0 {
			return errors.New("scm_size may not be unset or 0 when scm_class is ram")
		}
		if len(sc.DeviceList) > 0 {
			return errors.New("scm_list may not be set when scm_class is ram")
		}
	case ScmClassNone:
		return errors.New("scm_class not set")
	}

	if len(sc.DeviceList) > maxScmDeviceLen {
		return errors.Errorf("scm_list may have at most %d devices", maxScmDeviceLen)
	}
	return nil
}

const (
	BdevClassNone   BdevClass = ""
	BdevClassNvme   BdevClass = "nvme"
	BdevClassMalloc BdevClass = "malloc"
	BdevClassKdev   BdevClass = "kdev"
	BdevClassFile   BdevClass = "file"
)

// BdevClass specifies block device type for block device storage
type BdevClass string

// UnmarshalYAML implements yaml.Unmarshaler on BdevClass type
func (b *BdevClass) UnmarshalYAML(unmarshal func(interface{}) error) error {
	var class string
	if err := unmarshal(&class); err != nil {
		return err
	}
	bdevClass := BdevClass(class)
	switch bdevClass {
	// NB: It seems as if this is a valid default; configs generated by the test
	// harness have no bdev entries and are expected to work.
	case BdevClassNone:
		*b = BdevClassNvme
	case BdevClassNvme, BdevClassMalloc, BdevClassKdev, BdevClassFile:
		*b = bdevClass
	default:
		return errors.Errorf("bdev_class value %q not supported in config (nvme/malloc/kdev/file)", bdevClass)
	}
	return nil
}

func (b BdevClass) String() string {
	return string(b)
}

// BdevConfig represents a Block Device (NVMe, etc.) configuration entry.
type BdevConfig struct {
	ConfigPath  string    `yaml:"-" cmdLongFlag:"--nvme" cmdShortFlag:"-n"`
	Class       BdevClass `yaml:"bdev_class,omitempty"`
	DeviceList  []string  `yaml:"bdev_list,omitempty"`
	VmdDisabled bool      `yaml:"-"` // set during start-up
	DeviceCount int       `yaml:"bdev_number,omitempty"`
	FileSize    int       `yaml:"bdev_size,omitempty"`
	MemSize     int       `yaml:"-" cmdLongFlag:"--mem_size,nonzero" cmdShortFlag:"-r,nonzero"`
	VosEnv      string    `yaml:"-" cmdEnv:"VOS_BDEV_CLASS"`
	Hostname    string    `yaml:"-"` // used when generating templates
}

func (bc *BdevConfig) Validate() error {
	return nil
}

func (bc *BdevConfig) GetNvmeDevs() []string {
	if bc.Class == BdevClassNvme {
		return bc.DeviceList
	}

	return []string{}
}
