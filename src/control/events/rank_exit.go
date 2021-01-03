//
// (C) Copyright 2020 Intel Corporation.
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

package events

import (
	"time"

	"github.com/pkg/errors"

	"github.com/daos-stack/daos/src/control/common"
	"github.com/daos-stack/daos/src/control/common/proto/convert"
	mgmtpb "github.com/daos-stack/daos/src/control/common/proto/mgmt"
)

// RankStateInfo describes details of a rank's state.
type RankStateInfo struct {
	InstanceIdx uint32 `json:"instance_idx"`
	ExitErr     error  `json:"-"`
}

// RankExit is a custom event type that implements the Event interface.
type RankExit struct {
	RAS          *RASEvent
	ExtendedInfo *RankStateInfo
}

// GetID implements the method on the interface to return event ID.
func (evt *RankExit) GetID() RASID { return evt.RAS.ID }

// GetType implements the method on the interface to return event type.
func (evt *RankExit) GetType() RASTypeID { return evt.RAS.Type }

// FromProto unpacks protobuf RAS event into this RankExit instance, extracting
// ExtendedInfo variant into custom event specific fields.
func (evt *RankExit) FromProto(pbEvt *mgmtpb.RASEvent) error {
	evt.RAS = &RASEvent{
		Timestamp: pbEvt.Timestamp,
		Msg:       pbEvt.Msg,
		Hostname:  pbEvt.Hostname,
		Rank:      pbEvt.Rank,
		ID:        RASID(pbEvt.Id),
		Severity:  RASSeverityID(pbEvt.Severity),
		Type:      RASTypeID(pbEvt.Type),
	}

	pbInfo := pbEvt.GetRankStateInfo()
	if pbInfo == nil {
		return errors.Errorf("unexpected oneof, want %T got %T",
			&mgmtpb.RASEvent_RankStateInfo{}, pbInfo)
	}

	evt.ExtendedInfo = &RankStateInfo{
		InstanceIdx: pbInfo.GetInstance(),
	}
	if pbInfo.GetErrored() {
		evt.ExtendedInfo.ExitErr = common.ExitStatus(pbInfo.GetError())
	}

	return nil
}

// ToProto packs this RankExit instance into a protobuf RAS event, encoding
// custom event specific fields into the equivalent ExtendedInfo oneof variant.
func (evt *RankExit) ToProto() (*mgmtpb.RASEvent, error) {
	pbEvt := new(mgmtpb.RASEvent)
	if err := convert.Types(evt.RAS, pbEvt); err != nil {
		return nil, errors.Wrapf(err, "converting %T->%T", evt.RAS, pbEvt)
	}

	pbInfo := &mgmtpb.RankStateEventInfo{
		Instance: evt.ExtendedInfo.InstanceIdx,
	}
	if evt.ExtendedInfo.ExitErr != nil {
		pbInfo.Errored = true
		pbInfo.Error = evt.ExtendedInfo.ExitErr.Error()
	}

	pbEvt.ExtendedInfo = &mgmtpb.RASEvent_RankStateInfo{
		RankStateInfo: pbInfo,
	}

	return pbEvt, nil
}

// NewRankExitEvent creates a specific RankExit event from given inputs.
func NewRankExitEvent(hostname string, instanceIdx uint32, rank uint32, exitErr common.ExitStatus) Event {
	evt := &RASEvent{
		Timestamp: common.FormatTime(time.Now()),
		Msg:       "DAOS rank exited unexpectedly",
		ID:        RASRankExit,
		Hostname:  hostname,
		Rank:      rank,
		Type:      RASTypeStateChange,
		Severity:  RASSeverityError,
	}

	return &RankExit{
		RAS: evt,
		ExtendedInfo: &RankStateInfo{
			InstanceIdx: instanceIdx,
			ExitErr:     exitErr,
		},
	}
}